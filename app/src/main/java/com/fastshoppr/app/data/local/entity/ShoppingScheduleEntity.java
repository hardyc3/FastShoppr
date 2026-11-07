/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.fastshoppr.app.domain.model.IntervalUnit;

@Entity(tableName = "shopping_schedules")
public final class ShoppingScheduleEntity {

  @PrimaryKey @NonNull public final String id;

  public final int intervalValue;

  @NonNull public final IntervalUnit intervalUnit;

  @NonNull public final String nextShoppingDate;

  public final boolean isEnabled;

  public final boolean notifyOnCreate;

  @Nullable public final String lastCreatedDate;

  public final long createdAt;

  public final long updatedAt;

  public ShoppingScheduleEntity(
      @NonNull String id,
      int intervalValue,
      @NonNull IntervalUnit intervalUnit,
      @NonNull String nextShoppingDate,
      boolean isEnabled,
      boolean notifyOnCreate,
      @Nullable String lastCreatedDate,
      long createdAt,
      long updatedAt) {
    this.id = id;
    this.intervalValue = intervalValue;
    this.intervalUnit = intervalUnit;
    this.nextShoppingDate = nextShoppingDate;
    this.isEnabled = isEnabled;
    this.notifyOnCreate = notifyOnCreate;
    this.lastCreatedDate = lastCreatedDate;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
