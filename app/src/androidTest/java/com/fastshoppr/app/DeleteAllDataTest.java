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
import java.util.Locale;
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
  public void deleteAllDataClearsListsPrefsBranchesAndImagesButKeepsSeedStores() throws Exception {
    shoppingListDao.insert(
        new ShoppingListEntity(
            "list-to-delete", "Weekly", "2026-11-20", null, false, false, null, 1_000L, 1_000L));
    settingsPreferences.setAiCleanupEnabled(true);
    settingsPreferences.setLastCalendarMonth("2026-11");
    selectedStoreRepository.select(branch(StoreBrand.WALMART, "Walmart #123"));
    selectedStoreRepository.select(branch(StoreBrand.HOME_DEPOT, "Home Depot #456"));
    File image = captureImageStore.newImageFile();

    SharedPreferences settings =
        appContext.getSharedPreferences(SettingsPreferences.PREFS_NAME, Context.MODE_PRIVATE);
    String savedWalmartJson = settingsPreferences.getSelectedStoreJson(StoreBrand.WALMART);
    assertNotNull(savedWalmartJson);
    assertFalse(savedWalmartJson.contains("postalCode"));
    assertFalse(savedWalmartJson.contains("\"postal\""));
    assertNoZipKeys(settings);

    assertEquals(1, shoppingListDao.getActive().size());
    assertNotNull(settingsPreferences.getLastCalendarMonth());
    assertNotNull(selectedStoreRepository.getSelected(StoreBrand.WALMART));
    assertNotNull(selectedStoreRepository.getSelected(StoreBrand.HOME_DEPOT));

    userDataRepository.deleteAllData().get();

    assertEquals(0, shoppingListDao.getActive().size());
    assertNull(settingsPreferences.getLastCalendarMonth());
    assertFalse(settingsPreferences.isAiCleanupEnabled());
    assertNull(selectedStoreRepository.getSelected(StoreBrand.WALMART));
    assertNull(selectedStoreRepository.getSelected(StoreBrand.HOME_DEPOT));
    assertNull(settingsPreferences.getSelectedStoreJson(StoreBrand.WALMART));
    assertFalse(image.exists());
    assertEquals(4, storeDao.getActive().size());
    assertNoZipKeys(settings);
  }

  private static NearbyStore branch(StoreBrand brand, String name) {
    return new NearbyStore(
        "branch-" + brand.name(),
        brand,
        name,
        "1 Main St",
        "Denver",
        "CO",
        null,
        null,
        null,
        NearbyStoreSource.LOCATOR);
  }

  private static void assertNoZipKeys(SharedPreferences preferences) {
    for (String key : preferences.getAll().keySet()) {
      String lower = key.toLowerCase(Locale.US);
      assertFalse(lower.contains("postal"));
      assertFalse(lower.contains("zip"));
    }
  }
}
