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
import com.fastshoppr.app.domain.model.LookupSource;

@Entity(
    tableName = "aisle_cache",
    foreignKeys =
        @ForeignKey(
            entity = StoreEntity.class,
            parentColumns = "id",
            childColumns = "storeId",
            onDelete = ForeignKey.RESTRICT),
    indices = {
      @Index(
          value = {"storeId", "normalizedQuery"},
          unique = true)
    })
public final class AisleCacheEntity {

  @PrimaryKey @NonNull public final String id;

  @NonNull public final String storeId;

  @NonNull public final String normalizedQuery;

  @Nullable public final String aisle;

  @Nullable public final String bay;

  @Nullable public final String matchedProductName;

  @Nullable public final String matchedProductUrl;

  @Nullable public final Float confidence;

  @NonNull public final LookupSource source;

  public final long fetchedAt;

  public final long expiresAt;

  public AisleCacheEntity(
      @NonNull String id,
      @NonNull String storeId,
      @NonNull String normalizedQuery,
      @Nullable String aisle,
      @Nullable String bay,
      @Nullable String matchedProductName,
      @Nullable String matchedProductUrl,
      @Nullable Float confidence,
      @NonNull LookupSource source,
      long fetchedAt,
      long expiresAt) {
    this.id = id;
    this.storeId = storeId;
    this.normalizedQuery = normalizedQuery;
    this.aisle = aisle;
    this.bay = bay;
    this.matchedProductName = matchedProductName;
    this.matchedProductUrl = matchedProductUrl;
    this.confidence = confidence;
    this.source = source;
    this.fetchedAt = fetchedAt;
    this.expiresAt = expiresAt;
  }
}
