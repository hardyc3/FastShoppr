/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.preferences;

import android.content.SharedPreferences;
import com.fastshoppr.app.domain.model.SortDirection;

public final class SettingsPreferences {

  public static final String PREFS_NAME = "fastshoppr_settings";

  private static final String KEY_DEFAULT_STORE_ID = "default_store_id";
  private static final String KEY_LAST_USED_STORE_ID = "last_used_store_id";
  private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
  private static final String KEY_BACKGROUND_ORGANIZING_ENABLED = "background_organizing_enabled";
  private static final String KEY_AI_CLEANUP_ENABLED = "ai_cleanup_enabled";
  private static final String KEY_SORT_DIRECTION = "sort_direction";
  private static final String KEY_LAST_CALENDAR_MONTH = "last_calendar_month";
  private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
  private static final String KEY_WALMART_STORE_VERIFIED = "walmart_store_verified";
  private static final String KEY_PREFERRED_SHOPPING_TIME = "preferred_shopping_time";

  private final SharedPreferences preferences;

  public SettingsPreferences(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  public String getDefaultStoreId() {
    return preferences.getString(KEY_DEFAULT_STORE_ID, null);
  }

  public void setDefaultStoreId(String storeId) {
    preferences.edit().putString(KEY_DEFAULT_STORE_ID, storeId).apply();
  }

  public String getLastUsedStoreId() {
    return preferences.getString(KEY_LAST_USED_STORE_ID, null);
  }

  public void setLastUsedStoreId(String storeId) {
    preferences.edit().putString(KEY_LAST_USED_STORE_ID, storeId).apply();
  }

  public boolean areNotificationsEnabled() {
    return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);
  }

  public void setNotificationsEnabled(boolean enabled) {
    preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
  }

  public boolean isBackgroundOrganizingEnabled() {
    return preferences.getBoolean(KEY_BACKGROUND_ORGANIZING_ENABLED, false);
  }

  public void setBackgroundOrganizingEnabled(boolean enabled) {
    preferences.edit().putBoolean(KEY_BACKGROUND_ORGANIZING_ENABLED, enabled).apply();
  }

  public boolean isAiCleanupEnabled() {
    return preferences.getBoolean(KEY_AI_CLEANUP_ENABLED, false);
  }

  public void setAiCleanupEnabled(boolean enabled) {
    preferences.edit().putBoolean(KEY_AI_CLEANUP_ENABLED, enabled).apply();
  }

  public SortDirection getSortDirection() {
    String value = preferences.getString(KEY_SORT_DIRECTION, SortDirection.ASC.name());
    try {
      return SortDirection.valueOf(value);
    } catch (IllegalArgumentException | NullPointerException exception) {
      return SortDirection.ASC;
    }
  }

  public void setSortDirection(SortDirection direction) {
    preferences.edit().putString(KEY_SORT_DIRECTION, direction.name()).apply();
  }

  public String getLastCalendarMonth() {
    return preferences.getString(KEY_LAST_CALENDAR_MONTH, null);
  }

  public void setLastCalendarMonth(String yearMonth) {
    preferences.edit().putString(KEY_LAST_CALENDAR_MONTH, yearMonth).apply();
  }

  public boolean isOnboardingCompleted() {
    return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);
  }

  public void setOnboardingCompleted(boolean completed) {
    preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
  }

  public boolean isWalmartStoreVerified() {
    return preferences.getBoolean(KEY_WALMART_STORE_VERIFIED, false);
  }

  public void setWalmartStoreVerified(boolean verified) {
    preferences.edit().putBoolean(KEY_WALMART_STORE_VERIFIED, verified).apply();
  }

  public String getPreferredShoppingTime() {
    return preferences.getString(KEY_PREFERRED_SHOPPING_TIME, null);
  }

  public void setPreferredShoppingTime(String time) {
    preferences.edit().putString(KEY_PREFERRED_SHOPPING_TIME, time).apply();
  }

  /** Removes every stored setting, restoring all defaults. */
  public void clearAll() {
    if (!preferences.edit().clear().commit()) {
      throw new IllegalStateException("Could not persist settings clear");
    }
  }
}
