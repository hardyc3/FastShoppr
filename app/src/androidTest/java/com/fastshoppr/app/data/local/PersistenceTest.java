/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.id.UuidGenerator;
import com.fastshoppr.app.data.repository.ListItemRepositoryImpl;
import com.fastshoppr.app.data.repository.ShoppingListRepositoryImpl;
import com.fastshoppr.app.data.repository.StoreRepositoryImpl;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.testing.FakeClock;
import java.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class PersistenceTest {

  private static final String DATABASE_NAME = "persistence-test.db";

  private Context context;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase(DATABASE_NAME);
  }

  @After
  public void tearDown() {
    context.deleteDatabase(DATABASE_NAME);
  }

  @Test
  public void listItemAndSeedStoresSurviveDatabaseReopen() {
    FakeClock clock = new FakeClock();
    UuidGenerator idGenerator = new UuidGenerator();
    LocalDate shoppingDate = LocalDate.of(2026, 11, 7);

    FastShopprDatabase firstDatabase = openDatabase(clock);
    Store walmart =
        new StoreRepositoryImpl(firstDatabase, firstDatabase.storeDao(), clock)
            .findByBrand(StoreBrand.WALMART);
    ShoppingListRepository firstListRepository =
        new ShoppingListRepositoryImpl(
            firstDatabase,
            firstDatabase.shoppingListDao(),
            firstDatabase.listItemDao(),
            firstDatabase.captureSessionDao(),
            clock,
            idGenerator,
            null);
    ListItemRepository firstItemRepository =
        new ListItemRepositoryImpl(
            firstDatabase,
            firstDatabase.listItemDao(),
            firstDatabase.shoppingListDao(),
            clock,
            idGenerator);

    ShoppingList list = firstListRepository.getOrCreate(shoppingDate, "Weekly", null, false);
    ListItem item =
        ListItem.builder()
            .id(idGenerator.newId())
            .listId(list.getId())
            .storeId(walmart.getId())
            .rawText("  milk ")
            .displayName("milk")
            .targetDate(shoppingDate)
            .aisle("A12")
            .createdAt(clock.nowMillis())
            .updatedAt(clock.nowMillis())
            .build();
    firstItemRepository.insert(item);
    firstDatabase.close();

    FastShopprDatabase secondDatabase = openDatabase(clock);
    StoreRepository secondStoreRepository =
        new StoreRepositoryImpl(secondDatabase, secondDatabase.storeDao(), clock);
    ListItemRepository secondItemRepository =
        new ListItemRepositoryImpl(
            secondDatabase,
            secondDatabase.listItemDao(),
            secondDatabase.shoppingListDao(),
            clock,
            idGenerator);

    assertEquals(6, secondStoreRepository.getActive().size());
    ListItem loaded = secondItemRepository.getById(item.getId());
    assertNotNull(loaded);
    assertEquals("milk", loaded.getRawText());
    assertEquals("A12", loaded.getAisle());
    assertEquals(shoppingDate, loaded.getTargetDate());
    secondDatabase.close();
  }

  private FastShopprDatabase openDatabase(FakeClock clock) {
    return Room.databaseBuilder(context, FastShopprDatabase.class, DATABASE_NAME)
        .addCallback(new DatabaseSeedCallback(clock))
        .allowMainThreadQueries()
        .build();
  }
}
