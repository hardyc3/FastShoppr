/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.preferences;

import android.content.SharedPreferences;

public final class SettingsPreferences {

  public static final String PREFS_NAME = "fastshoppr_settings";

  private static final String KEY_DARK_MODE = "dark_mode";
  private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
  private static final String KEY_SCHEDULE_INTERVAL_DAYS = "schedule_interval_days";
  private static final String KEY_LAST_STORE_ID = "last_store_id";

  private final SharedPreferences preferences;

  public SettingsPreferences(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  public boolean isDarkMode() {
    return preferences.getBoolean(KEY_DARK_MODE, false);
  }

  public void setDarkMode(boolean enabled) {
    preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
  }

  public boolean areNotificationsEnabled() {
    return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);
  }

  public void setNotificationsEnabled(boolean enabled) {
    preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
  }

  public int getScheduleIntervalDays() {
    return preferences.getInt(KEY_SCHEDULE_INTERVAL_DAYS, 7);
  }

  public void setScheduleIntervalDays(int days) {
    preferences.edit().putInt(KEY_SCHEDULE_INTERVAL_DAYS, days).apply();
  }

  public String getLastStoreId() {
    return preferences.getString(KEY_LAST_STORE_ID, null);
  }

  public void setLastStoreId(String storeId) {
    preferences.edit().putString(KEY_LAST_STORE_ID, storeId).apply();
  }
}
