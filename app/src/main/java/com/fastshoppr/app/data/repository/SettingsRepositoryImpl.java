/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import com.fastshoppr.app.data.preferences.SettingsPreferences;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SettingsRepositoryImpl implements SettingsRepository {

  private final SettingsPreferences preferences;

  @Inject
  public SettingsRepositoryImpl(SettingsPreferences preferences) {
    this.preferences = preferences;
  }

  @Override
  public String getDefaultStoreId() {
    return preferences.getDefaultStoreId();
  }

  @Override
  public void setDefaultStoreId(String storeId) {
    preferences.setDefaultStoreId(storeId);
  }

  @Override
  public String getLastUsedStoreId() {
    return preferences.getLastUsedStoreId();
  }

  @Override
  public void setLastUsedStoreId(String storeId) {
    preferences.setLastUsedStoreId(storeId);
  }

  @Override
  public boolean areNotificationsEnabled() {
    return preferences.areNotificationsEnabled();
  }

  @Override
  public void setNotificationsEnabled(boolean enabled) {
    preferences.setNotificationsEnabled(enabled);
  }

  @Override
  public boolean isBackgroundOrganizingEnabled() {
    return preferences.isBackgroundOrganizingEnabled();
  }

  @Override
  public void setBackgroundOrganizingEnabled(boolean enabled) {
    preferences.setBackgroundOrganizingEnabled(enabled);
  }

  @Override
  public boolean isAiCleanupEnabled() {
    return preferences.isAiCleanupEnabled();
  }

  @Override
  public void setAiCleanupEnabled(boolean enabled) {
    preferences.setAiCleanupEnabled(enabled);
  }

  @Override
  public SortDirection getSortDirection() {
    return preferences.getSortDirection();
  }

  @Override
  public void setSortDirection(SortDirection direction) {
    preferences.setSortDirection(direction);
  }

  @Override
  public String getLastCalendarMonth() {
    return preferences.getLastCalendarMonth();
  }

  @Override
  public void setLastCalendarMonth(String yearMonth) {
    preferences.setLastCalendarMonth(yearMonth);
  }

  @Override
  public boolean isOnboardingCompleted() {
    return preferences.isOnboardingCompleted();
  }

  @Override
  public void setOnboardingCompleted(boolean completed) {
    preferences.setOnboardingCompleted(completed);
  }
}
