/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.SortDirection;

public interface SettingsRepository {

  String getDefaultStoreId();

  void setDefaultStoreId(String storeId);

  String getLastUsedStoreId();

  void setLastUsedStoreId(String storeId);

  boolean areNotificationsEnabled();

  void setNotificationsEnabled(boolean enabled);

  boolean isBackgroundOrganizingEnabled();

  void setBackgroundOrganizingEnabled(boolean enabled);

  boolean isAiCleanupEnabled();

  void setAiCleanupEnabled(boolean enabled);

  SortDirection getSortDirection();

  void setSortDirection(SortDirection direction);

  String getLastCalendarMonth();

  void setLastCalendarMonth(String yearMonth);

  boolean isOnboardingCompleted();

  void setOnboardingCompleted(boolean completed);

  boolean isWalmartStoreVerified();

  void setWalmartStoreVerified(boolean verified);
}
