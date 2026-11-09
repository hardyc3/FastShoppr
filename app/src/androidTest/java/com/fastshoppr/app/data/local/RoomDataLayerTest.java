/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.id.UuidGenerator;
import com.fastshoppr.app.data.local.dao.CaptureSessionDao;
import com.fastshoppr.app.data.local.dao.ListItemDao;
import com.fastshoppr.app.data.local.dao.ShoppingListDao;
import com.fastshoppr.app.data.local.dao.StoreDao;
import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import com.fastshoppr.app.data.repository.CaptureRepositoryImpl;
import com.fastshoppr.app.data.repository.ListItemRepositoryImpl;
import com.fastshoppr.app.data.repository.ShoppingListRepositoryImpl;
import com.fastshoppr.app.data.repository.StoreRepositoryImpl;
import com.fastshoppr.app.domain.error.ConflictException;
import com.fastshoppr.app.domain.error.StoreInUseException;
import com.fastshoppr.app.domain.model.CaptureSession;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.testing.FakeClock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class RoomDataLayerTest {

  private static final LocalDate DATE_ONE = LocalDate.of(2026, 11, 7);
  private static final LocalDate DATE_TWO = LocalDate.of(2026, 11, 14);
  private static final LocalDate DATE_THREE = LocalDate.of(2026, 11, 21);

  private FastShopprDatabase database;
  private ShoppingListDao listDao;
  private ListItemDao itemDao;
  private CaptureSessionDao captureDao;
  private StoreDao storeDao;
  private ShoppingListRepository listRepository;
  private ListItemRepository itemRepository;
  private StoreRepository storeRepository;
  private CaptureRepository captureRepository;
  private UuidGenerator idGenerator;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    database =
        androidx.room.Room.inMemoryDatabaseBuilder(context, FastShopprDatabase.class)
            .addCallback(new DatabaseSeedCallback(new FakeClock()))
            .allowMainThreadQueries()
            .build();
    listDao = database.shoppingListDao();
    itemDao = database.listItemDao();
    captureDao = database.captureSessionDao();
    storeDao = database.storeDao();
    idGenerator = new UuidGenerator();
    listRepository =
        new ShoppingListRepositoryImpl(
            database, listDao, itemDao, captureDao, new FakeClock(), idGenerator);
    itemRepository =
        new ListItemRepositoryImpl(database, itemDao, listDao, new FakeClock(), idGenerator);
    storeRepository = new StoreRepositoryImpl(database, storeDao, new FakeClock());
    captureRepository = new CaptureRepositoryImpl(captureDao, new FakeClock());
    storeDao.getActive();
  }

  @After
  public void tearDown() {
    database.close();
  }

  @Test
  public void seedsFiveChainStoresExactlyOnce() {
    List<Store> firstRead = storeRepository.getActive();
    List<Store> secondRead = storeRepository.getActive();

    assertEquals(5, firstRead.size());
    assertEquals(5, secondRead.size());
    Set<StoreBrand> brands = firstRead.stream().map(Store::getBrand).collect(Collectors.toSet());
    assertEquals(
        Set.of(
            StoreBrand.WALMART,
            StoreBrand.HOME_DEPOT,
            StoreBrand.LOWES,
            StoreBrand.TARGET,
            StoreBrand.SMITHS),
        brands);
    assertTrue(firstRead.stream().allMatch(store -> !store.isCustom()));
  }

  @Test
  public void getOrCreateReturnsExistingListForDate() {
    ShoppingList first = listRepository.getOrCreate(DATE_ONE, "First", null, false);
    ShoppingList second = listRepository.getOrCreate(DATE_ONE, "Second", null, true);

    assertEquals(first.getId(), second.getId());
    assertEquals("First", second.getName());
    assertEquals(1, listRepository.getActive().size());
  }

  @Test
  public void moveItemMovesToListTransactionallyAndCreatesMissingList() {
    ShoppingList firstList = listRepository.getOrCreate(DATE_ONE, "First", null, false);
    ShoppingList secondList = listRepository.getOrCreate(DATE_TWO, "Second", null, false);
    ListItem item = itemRepositoryItem(firstList.getId(), DATE_ONE, 0);
    itemRepository.insert(item);

    ListItem moved = itemRepository.moveToDate(item.getId(), DATE_TWO, "Second");

    assertEquals(secondList.getId(), moved.getListId());
    assertEquals(DATE_TWO, moved.getTargetDate());
    assertTrue(itemRepository.getByList(firstList.getId()).isEmpty());
    assertEquals(1, itemRepository.getByList(secondList.getId()).size());

    ListItem itemForNewList = itemRepositoryItem(firstList.getId(), DATE_ONE, 1);
    itemRepository.insert(itemForNewList);

    ListItem movedToNewList =
        itemRepository.moveToDate(itemForNewList.getId(), DATE_THREE, "Third");

    ShoppingListEntity created = listDao.findActiveByDate(DATE_THREE.toString());
    assertNotNull(created);
    assertEquals("Third", created.name);
    assertEquals(created.id, movedToNewList.getListId());
    assertEquals(DATE_THREE, movedToNewList.getTargetDate());
  }

  @Test
  public void softDeleteCascadesRestoreAndPurge() {
    ShoppingList list = listRepository.getOrCreate(DATE_ONE, "First", null, false);
    ListItem item = itemRepositoryItem(list.getId(), DATE_ONE, 0);
    itemRepository.insert(item);
    CaptureSession capture = capture(list.getId());
    captureRepository.insert(capture);

    listRepository.softDelete(list.getId());

    assertNull(listDao.getById(list.getId()));
    assertNull(itemDao.getById(item.getId()));
    assertNull(captureDao.getById(capture.getId()));
    assertNotNull(listDao.getByIdIncludingDeleted(list.getId()).deletedAt);
    assertNotNull(itemDao.getByIdIncludingDeleted(item.getId()).deletedAt);
    assertNotNull(captureDao.getByIdIncludingDeleted(capture.getId()).deletedAt);

    listRepository.restore(list.getId());

    assertNotNull(listDao.getById(list.getId()));
    assertNotNull(itemDao.getById(item.getId()));
    assertNotNull(captureDao.getById(capture.getId()));

    listRepository.softDelete(list.getId());
    listRepository.purgeDeletedBefore(FakeClock.NOW + 1);

    assertNull(listDao.getByIdIncludingDeleted(list.getId()));
    assertNull(itemDao.getByIdIncludingDeleted(item.getId()));
    assertNull(captureDao.getByIdIncludingDeleted(capture.getId()));
  }

  @Test
  public void archivedListLeavesActiveSetAndNewListCanUseDate() {
    ShoppingList first = listRepository.getOrCreate(DATE_ONE, "First", null, false);

    listRepository.archive(first.getId());

    assertTrue(listRepository.getActive().isEmpty());
    assertNotNull(listDao.getById(first.getId()));

    ShoppingList second = listRepository.getOrCreate(DATE_ONE, "Second", null, false);
    assertNotEquals(first.getId(), second.getId());
    assertEquals(1, listRepository.getActive().size());
  }

  @Test
  public void restoringDeletedListConflictsWithNewActiveListOnSameDate() {
    ShoppingList first = listRepository.getOrCreate(DATE_ONE, "First", null, false);
    listRepository.softDelete(first.getId());
    listRepository.getOrCreate(DATE_ONE, "Second", null, false);

    assertThrows(ConflictException.class, () -> listRepository.restore(first.getId()));
  }

  @Test
  public void defaultStoreSelectionIsExclusive() {
    Store walmart = storeRepository.findByBrand(StoreBrand.WALMART);
    Store target = storeRepository.findByBrand(StoreBrand.TARGET);

    storeRepository.setDefault(walmart.getId());
    assertEquals(walmart.getId(), storeRepository.getDefault().getId());

    storeRepository.setDefault(target.getId());
    assertEquals(target.getId(), storeRepository.getDefault().getId());
    assertEquals(1, storeRepository.getActive().stream().filter(Store::isDefault).count());
  }

  @Test
  public void storeDeleteIsBlockedWhileReferencedAndArchiveWorks() {
    ShoppingList list = listRepository.getOrCreate(DATE_ONE, "First", null, false);
    ListItem item = itemRepositoryItem(list.getId(), DATE_ONE, 0);
    itemRepository.insert(item);

    Store walmart = storeRepository.findByBrand(StoreBrand.WALMART);
    assertThrows(StoreInUseException.class, () -> storeRepository.delete(walmart.getId()));

    storeRepository.archive(walmart.getId());
    assertTrue(
        storeRepository.getActive().stream()
            .noneMatch(store -> store.getId().equals(walmart.getId())));
    assertTrue(storeRepository.getById(walmart.getId()).isArchived());
  }

  private ListItem itemRepositoryItem(String listId, LocalDate targetDate, int sortPosition) {
    String storeId = storeRepository.findByBrand(StoreBrand.WALMART).getId();
    return ListItem.builder()
        .id(idGenerator.newId())
        .listId(listId)
        .storeId(storeId)
        .rawText("milk")
        .displayName("milk")
        .normalizedText("milk")
        .targetDate(targetDate)
        .sortPosition(sortPosition)
        .createdAt(FakeClock.NOW)
        .updatedAt(FakeClock.NOW)
        .build();
  }

  private CaptureSession capture(String listId) {
    return new CaptureSession(
        idGenerator.newId(), listId, ItemSource.TYPED, null, "[]", FakeClock.NOW, null, null);
  }
}
