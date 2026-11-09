/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public final class ShoppingSchedule {

  private final String id;
  private final int intervalValue;
  private final IntervalUnit intervalUnit;
  private final LocalDate nextShoppingDate;
  private final boolean isEnabled;
  private final boolean notifyOnCreate;
  private final LocalDate lastCreatedDate;
  private final long createdAt;
  private final long updatedAt;

  public ShoppingSchedule(
      String id,
      int intervalValue,
      IntervalUnit intervalUnit,
      LocalDate nextShoppingDate,
      boolean isEnabled,
      boolean notifyOnCreate,
      LocalDate lastCreatedDate,
      long createdAt,
      long updatedAt) {
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("schedule id must not be blank");
    }
    if (intervalValue < 1) {
      throw new IllegalArgumentException("intervalValue must be at least 1");
    }
    this.id = id.trim();
    this.intervalValue = intervalValue;
    this.intervalUnit = Objects.requireNonNull(intervalUnit, "intervalUnit must not be null");
    this.nextShoppingDate =
        Objects.requireNonNull(nextShoppingDate, "nextShoppingDate must not be null");
    this.isEnabled = isEnabled;
    this.notifyOnCreate = notifyOnCreate;
    this.lastCreatedDate = lastCreatedDate;
    if (createdAt < 0) {
      throw new IllegalArgumentException("createdAt must not be negative");
    }
    if (updatedAt < 0) {
      throw new IllegalArgumentException("updatedAt must not be negative");
    }
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public int getIntervalValue() {
    return intervalValue;
  }

  public IntervalUnit getIntervalUnit() {
    return intervalUnit;
  }

  public LocalDate getNextShoppingDate() {
    return nextShoppingDate;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public boolean isNotifyOnCreate() {
    return notifyOnCreate;
  }

  public LocalDate getLastCreatedDate() {
    return lastCreatedDate;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }
}
