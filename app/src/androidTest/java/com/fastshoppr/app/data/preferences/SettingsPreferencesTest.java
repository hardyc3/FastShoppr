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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class SettingsPreferencesTest {

  private SettingsPreferences settings;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    SharedPreferences preferences =
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

    assertEquals("default-store", settings.getDefaultStoreId());
    assertEquals("last-store", settings.getLastUsedStoreId());
    assertTrue(settings.areNotificationsEnabled());
    assertTrue(settings.isBackgroundOrganizingEnabled());
    assertTrue(settings.isAiCleanupEnabled());
    assertEquals(SortDirection.DESC, settings.getSortDirection());
    assertEquals("2026-11", settings.getLastCalendarMonth());
    assertTrue(settings.isOnboardingCompleted());
  }
}
