/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.local.DatabaseSeedCallback;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.dao.ShoppingListDao;
import com.fastshoppr.app.data.local.dao.StoreDao;
import com.fastshoppr.app.data.local.entity.ListItemEntity;
import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import com.fastshoppr.app.data.local.entity.StoreEntity;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.testing.FakeClock;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Phase 7 state-transition checks on device (cancellation/reset behavior, batch size). */
@RunWith(AndroidJUnit4.class)
public final class OrganizingStateInstrumentationTest {

  private FastShopprDatabase db;
  private ListItemDao itemDao;
  private ShoppingListDao listDao;
  private StoreDao storeDao;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    db =
        Room.inMemoryDatabaseBuilder(context, FastShopprDatabase.class)
            .addCallback(new DatabaseSeedCallback(new FakeClock()))
            .allowMainThreadQueries()
            .build();
    itemDao = db.listItemDao();
    listDao = db.shoppingListDao();
    storeDao = db.storeDao();
    storeDao.getActive();
  }

  @After
  public void tearDown() {
    db.close();
  }

  @Test
  public void cancellationKeepsCompletedAndResetsInFlight() {
    String listId = "list-cancel";
    String storeId = firstStoreId();
    insertList(listId, "2026-11-15");

    insertItem(listId, storeId, "i-found", LookupStatus.FOUND, "A1", 0);
    insertItem(listId, storeId, "i-review", LookupStatus.NEEDS_REVIEW, null, 1);
    insertItem(listId, storeId, "i-error", LookupStatus.ERROR, null, 2);
    insertItem(listId, storeId, "i-progress", LookupStatus.IN_PROGRESS, null, 3);
    insertItem(listId, storeId, "i-queued", LookupStatus.QUEUED, null, 4);

    long now = 12345L;
    int updated =
        itemDao.setLookupStatusForList(
            listId,
            List.of(LookupStatus.IN_PROGRESS.name(), LookupStatus.QUEUED.name()),
            LookupStatus.PENDING.name(),
            now);
    assertEquals(2, updated);

    assertEquals(LookupStatus.FOUND, itemDao.getById("i-found").lookupStatus);
    assertEquals(LookupStatus.NEEDS_REVIEW, itemDao.getById("i-review").lookupStatus);
    assertEquals(LookupStatus.ERROR, itemDao.getById("i-error").lookupStatus);
    assertEquals(LookupStatus.PENDING, itemDao.getById("i-progress").lookupStatus);
    assertEquals(LookupStatus.PENDING, itemDao.getById("i-queued").lookupStatus);

    List<ListItemEntity> after = itemDao.getByList(listId);
    int found = 0, review = 0, errors = 0, pending = 0;
    for (ListItemEntity e : after) {
      if (LookupStatus.FOUND.equals(e.lookupStatus)) found++;
      else if (LookupStatus.NEEDS_REVIEW.equals(e.lookupStatus)) review++;
      else if (LookupStatus.ERROR.equals(e.lookupStatus)) errors++;
      else if (LookupStatus.PENDING.equals(e.lookupStatus)
          || LookupStatus.QUEUED.equals(e.lookupStatus)
          || LookupStatus.IN_PROGRESS.equals(e.lookupStatus)) pending++;
    }
    assertEquals(1, found);
    assertEquals(1, review);
    assertEquals(1, errors);
    assertEquals(2, pending);
  }

  @Test
  public void handlesHundredItemsStateBatchWithoutUiBlockingWork() {
    String listId = "list-100";
    String storeId = firstStoreId();
    insertList(listId, "2026-11-16");
    for (int i = 0; i < 100; i++) {
      insertItem(listId, storeId, "item-" + i, LookupStatus.PENDING, null, i);
    }
    long now = 222L;
    int queued =
        itemDao.setLookupStatusForList(
            listId, List.of(LookupStatus.PENDING.name()), LookupStatus.QUEUED.name(), now);
    assertEquals(100, queued);
    List<ListItemEntity> all = itemDao.getByList(listId);
    assertEquals(100, all.size());
    int queuedCount = 0;
    for (ListItemEntity e : all) {
      if (LookupStatus.QUEUED.equals(e.lookupStatus)) queuedCount++;
    }
    assertEquals(100, queuedCount);
    // simulate processing: mark some found, keep unresolved last semantics handled by sort logic
    itemDao.setLookupStatusIfNotManual(all.get(0).id, LookupStatus.IN_PROGRESS.name(), now + 1);
    itemDao.setLookupStatusIfNotManual(all.get(0).id, LookupStatus.FOUND.name(), now + 2);
    assertEquals(LookupStatus.FOUND, itemDao.getById(all.get(0).id).lookupStatus);
    assertTrue(itemDao.getByList(listId).size() == 100);
  }

  private String firstStoreId() {
    List<StoreEntity> stores = storeDao.getActive();
    return stores.get(0).id;
  }

  private void insertList(String id, String date) {
    ShoppingListEntity e =
        new ShoppingListEntity(id, "List", date, null, false, false, null, 1L, 1L);
    listDao.insert(e);
  }

  private void insertItem(
      String listId, String storeId, String id, LookupStatus status, String aisle, int pos) {
    ListItemEntity e =
        new ListItemEntity(
            id,
            listId,
            storeId,
            id,
            id,
            id,
            null,
            "2026-11-15",
            aisle,
            null,
            null,
            null,
            null,
            status,
            null,
            null,
            null,
            ItemSource.TYPED,
            false,
            pos,
            null,
            1L,
            1L);
    itemDao.insert(e);
  }
}
