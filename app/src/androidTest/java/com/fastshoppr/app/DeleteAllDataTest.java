/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.data.local.FastShopprDatabase;
import com.fastshoppr.app.data.local.dao.ShoppingListDao;
import com.fastshoppr.app.data.local.dao.StoreDao;
import com.fastshoppr.app.data.local.entity.ShoppingListEntity;
import com.fastshoppr.app.data.preferences.SettingsPreferences;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import com.fastshoppr.app.domain.repository.UserDataRepository;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.io.File;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public final class DeleteAllDataTest {

  @Rule public final HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject UserDataRepository userDataRepository;
  @Inject FastShopprDatabase database;
  @Inject ShoppingListDao shoppingListDao;
  @Inject StoreDao storeDao;
  @Inject SettingsPreferences settingsPreferences;
  @Inject SelectedStoreRepository selectedStoreRepository;
  @Inject CaptureImageStore captureImageStore;
  @Inject @ApplicationContext Context appContext;

  @Before
  public void setUp() {
    hiltRule.inject();
    clearUserData();
    database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM stores");
  }

  @After
  public void tearDown() {
    clearUserData();
  }

  private void clearUserData() {
    SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
    sqlite.execSQL("DELETE FROM list_items");
    sqlite.execSQL("DELETE FROM capture_sessions");
    sqlite.execSQL("DELETE FROM shopping_lists");
    sqlite.execSQL("DELETE FROM shopping_schedules");
    sqlite.execSQL("DELETE FROM aisle_cache");
  }

  @Test
  public void deleteAllDataClearsListsPrefsAndImagesButKeepsSeedStores() throws Exception {
    shoppingListDao.insert(
        new ShoppingListEntity(
            "list-to-delete", "Weekly", "2026-11-20", null, false, false, null, 1_000L, 1_000L));
    settingsPreferences.setAiCleanupEnabled(true);
    settingsPreferences.setLastCalendarMonth("2026-11");
    selectedStoreRepository.setPostalCode("90210");
    selectedStoreRepository.select(
        new NearbyStore(
            "123",
            StoreBrand.WALMART,
            "Walmart #123",
            null,
            null,
            null,
            "90210",
            null,
            null,
            NearbyStoreSource.MANUAL));
    File image = captureImageStore.newImageFile();
    SharedPreferences locatorCache =
        appContext.getSharedPreferences("fastshoppr_store_locator_cache", Context.MODE_PRIVATE);
    locatorCache.edit().putString("stale-entry", "cached").commit();

    assertEquals(1, shoppingListDao.getActive().size());
    assertTrue(locatorCache.contains("stale-entry"));
    assertNotNull(settingsPreferences.getLastCalendarMonth());
    assertNotNull(selectedStoreRepository.getSelected(StoreBrand.WALMART));

    userDataRepository.deleteAllData().get();

    assertEquals(0, shoppingListDao.getActive().size());
    assertNull(settingsPreferences.getLastCalendarMonth());
    assertFalse(settingsPreferences.isAiCleanupEnabled());
    assertNull(selectedStoreRepository.getPostalCode());
    assertNull(selectedStoreRepository.getSelected(StoreBrand.WALMART));
    assertFalse(image.exists());
    assertEquals(6, storeDao.getActive().size());
  }
}
