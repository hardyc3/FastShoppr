/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "shopping_lists",
    foreignKeys =
        @ForeignKey(
            entity = ShoppingScheduleEntity.class,
            parentColumns = "id",
            childColumns = "scheduleId",
            onDelete = ForeignKey.SET_NULL),
    indices = {
      @Index("shoppingDate"),
      @Index("scheduleId"),
      @Index("isArchived"),
      @Index("deletedAt")
    })
public final class ShoppingListEntity {

  @PrimaryKey @NonNull public final String id;

  @NonNull public final String name;

  @NonNull public final String shoppingDate;

  @Nullable public final String scheduleId;

  public final boolean isAutoCreated;

  public final boolean isArchived;

  @Nullable public final Long deletedAt;

  public final long createdAt;

  public final long updatedAt;

  public ShoppingListEntity(
      @NonNull String id,
      @NonNull String name,
      @NonNull String shoppingDate,
      @Nullable String scheduleId,
      boolean isAutoCreated,
      boolean isArchived,
      @Nullable Long deletedAt,
      long createdAt,
      long updatedAt) {
    this.id = id;
    this.name = name;
    this.shoppingDate = shoppingDate;
    this.scheduleId = scheduleId;
    this.isAutoCreated = isAutoCreated;
    this.isArchived = isArchived;
    this.deletedAt = deletedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
