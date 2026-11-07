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
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;

@Entity(
    tableName = "list_items",
    foreignKeys = {
      @ForeignKey(
          entity = ShoppingListEntity.class,
          parentColumns = "id",
          childColumns = "listId",
          onDelete = ForeignKey.CASCADE),
      @ForeignKey(
          entity = StoreEntity.class,
          parentColumns = "id",
          childColumns = "storeId",
          onDelete = ForeignKey.RESTRICT)
    },
    indices = {
      @Index("listId"),
      @Index("storeId"),
      @Index("targetDate"),
      @Index("lookupStatus"),
      @Index("deletedAt")
    })
public final class ListItemEntity {

  @PrimaryKey @NonNull public final String id;

  @NonNull public final String listId;

  @NonNull public final String storeId;

  @NonNull public final String rawText;

  @NonNull public final String displayName;

  @NonNull public final String normalizedText;

  @Nullable public final String quantity;

  @NonNull public final String targetDate;

  @Nullable public final String aisle;

  @Nullable public final String bay;

  @Nullable public final String matchedProductName;

  @Nullable public final String matchedProductUrl;

  @Nullable public final Float matchConfidence;

  @NonNull public final LookupStatus lookupStatus;

  @Nullable public final LookupSource lookupSource;

  @Nullable public final String lookupError;

  @NonNull public final ItemSource source;

  public final boolean isChecked;

  public final int sortPosition;

  @Nullable public final Long deletedAt;

  public final long createdAt;

  public final long updatedAt;

  public ListItemEntity(
      @NonNull String id,
      @NonNull String listId,
      @NonNull String storeId,
      @NonNull String rawText,
      @NonNull String displayName,
      @NonNull String normalizedText,
      @Nullable String quantity,
      @NonNull String targetDate,
      @Nullable String aisle,
      @Nullable String bay,
      @Nullable String matchedProductName,
      @Nullable String matchedProductUrl,
      @Nullable Float matchConfidence,
      @NonNull LookupStatus lookupStatus,
      @Nullable LookupSource lookupSource,
      @Nullable String lookupError,
      @NonNull ItemSource source,
      boolean isChecked,
      int sortPosition,
      @Nullable Long deletedAt,
      long createdAt,
      long updatedAt) {
    this.id = id;
    this.listId = listId;
    this.storeId = storeId;
    this.rawText = rawText;
    this.displayName = displayName;
    this.normalizedText = normalizedText;
    this.quantity = quantity;
    this.targetDate = targetDate;
    this.aisle = aisle;
    this.bay = bay;
    this.matchedProductName = matchedProductName;
    this.matchedProductUrl = matchedProductUrl;
    this.matchConfidence = matchConfidence;
    this.lookupStatus = lookupStatus;
    this.lookupSource = lookupSource;
    this.lookupError = lookupError;
    this.source = source;
    this.isChecked = isChecked;
    this.sortPosition = sortPosition;
    this.deletedAt = deletedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
