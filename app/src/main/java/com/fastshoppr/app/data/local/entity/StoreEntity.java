/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.fastshoppr.app.domain.model.StoreBrand;

@Entity(
    tableName = "stores",
    indices = {@Index("brand")})
public final class StoreEntity {

  @PrimaryKey @NonNull public final String id;

  @NonNull public final StoreBrand brand;

  @NonNull public final String name;

  @Nullable public final String addressLine;

  @Nullable public final String city;

  @Nullable public final String state;

  @Nullable public final String postalCode;

  @Nullable public final Double latitude;

  @Nullable public final Double longitude;

  public final boolean isCustom;

  public final boolean isDefault;

  public final boolean isArchived;

  @Nullable public final String hoursJson;

  public final long createdAt;

  public final long updatedAt;

  public StoreEntity(
      @NonNull String id,
      @NonNull StoreBrand brand,
      @NonNull String name,
      @Nullable String addressLine,
      @Nullable String city,
      @Nullable String state,
      @Nullable String postalCode,
      @Nullable Double latitude,
      @Nullable Double longitude,
      boolean isCustom,
      boolean isDefault,
      boolean isArchived,
      @Nullable String hoursJson,
      long createdAt,
      long updatedAt) {
    this.id = id;
    this.brand = brand;
    this.name = name;
    this.addressLine = addressLine;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
    this.latitude = latitude;
    this.longitude = longitude;
    this.isCustom = isCustom;
    this.isDefault = isDefault;
    this.isArchived = isArchived;
    this.hoursJson = hoursJson;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
