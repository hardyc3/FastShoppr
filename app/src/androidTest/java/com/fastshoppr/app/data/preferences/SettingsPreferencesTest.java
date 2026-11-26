/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class SettingsPreferencesTest {

  private SharedPreferences preferences;
  private SettingsPreferences settings;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    preferences =
        context.getSharedPreferences("settings-test-" + System.nanoTime(), Context.MODE_PRIVATE);
    preferences.edit().clear().commit();
    settings = new SettingsPreferences(preferences);
  }

  @Test
  public void defaultsAreStable() {
    assertNull(settings.getDefaultStoreId());
    assertNull(settings.getLastUsedStoreId());
    assertFalse(settings.areNotificationsEnabled());
    assertFalse(settings.isBackgroundOrganizingEnabled());
    assertFalse(settings.isAiCleanupEnabled());
    assertEquals(SortDirection.ASC, settings.getSortDirection());
    assertNull(settings.getLastCalendarMonth());
    assertFalse(settings.isOnboardingCompleted());
    assertNull(settings.getSelectedStoreJson(StoreBrand.WALMART));
    assertNoZipKeys();
  }

  @Test
  public void readsAndWritesEveryKey() {
    settings.setDefaultStoreId("default-store");
    settings.setLastUsedStoreId("last-store");
    settings.setNotificationsEnabled(true);
    settings.setBackgroundOrganizingEnabled(true);
    settings.setAiCleanupEnabled(true);
    settings.setSortDirection(SortDirection.DESC);
    settings.setLastCalendarMonth("2026-11");
    settings.setOnboardingCompleted(true);
    settings.setSelectedStoreJson(StoreBrand.WALMART, branchJson("Walmart #1"));
    settings.setSelectedStoreJson(StoreBrand.HOME_DEPOT, branchJson("Home Depot #2"));

    assertEquals("default-store", settings.getDefaultStoreId());
    assertEquals("last-store", settings.getLastUsedStoreId());
    assertTrue(settings.areNotificationsEnabled());
    assertTrue(settings.isBackgroundOrganizingEnabled());
    assertTrue(settings.isAiCleanupEnabled());
    assertEquals(SortDirection.DESC, settings.getSortDirection());
    assertEquals("2026-11", settings.getLastCalendarMonth());
    assertTrue(settings.isOnboardingCompleted());
    assertEquals(branchJson("Walmart #1"), settings.getSelectedStoreJson(StoreBrand.WALMART));
    assertEquals(branchJson("Home Depot #2"), settings.getSelectedStoreJson(StoreBrand.HOME_DEPOT));
    assertNoZipKeys();
  }

  @Test
  public void storesOneBranchPerBrandUnderSeparateKeys() {
    for (StoreBrand brand : StoreBrand.values()) {
      settings.setSelectedStoreJson(brand, branchJson(brand.getDisplayName() + " branch"));
    }

    for (StoreBrand brand : StoreBrand.values()) {
      assertEquals(
          branchJson(brand.getDisplayName() + " branch"), settings.getSelectedStoreJson(brand));
      assertTrue(preferences.contains("selected_store_" + brand.name()));
    }
    assertNoZipKeys();
  }

  @Test
  public void clearAllRestoresDefaults() {
    settings.setAiCleanupEnabled(true);
    settings.setSelectedStoreJson(StoreBrand.SMITHS, branchJson("Smiths #1"));
    settings.setDefaultStoreId("default-store");

    settings.clearAll();

    assertNull(settings.getDefaultStoreId());
    assertFalse(settings.isAiCleanupEnabled());
    assertNull(settings.getSelectedStoreJson(StoreBrand.SMITHS));
    assertTrue(preferences.getAll().isEmpty());
    assertNoZipKeys();
  }

  private void assertNoZipKeys() {
    for (String key : preferences.getAll().keySet()) {
      String lower = key.toLowerCase(Locale.US);
      assertFalse(lower.contains("postal"));
      assertFalse(lower.contains("zip"));
    }
  }

  private static String branchJson(String name) {
    return "{\"id\":\"1\",\"name\":\"" + name + "\",\"source\":\"LOCATOR\"}";
  }
}
