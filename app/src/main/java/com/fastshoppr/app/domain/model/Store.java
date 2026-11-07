/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import com.fastshoppr.app.domain.text.TextNormalizer;
import java.util.Objects;

public final class Store {

  private final String id;
  private final StoreBrand brand;
  private final String name;
  private final String addressLine;
  private final String city;
  private final String state;
  private final String postalCode;
  private final Double latitude;
  private final Double longitude;
  private final boolean isCustom;
  private final boolean isDefault;
  private final boolean isArchived;
  private final String hoursJson;
  private final long createdAt;
  private final long updatedAt;

  public Store(
      String id,
      StoreBrand brand,
      String name,
      String addressLine,
      String city,
      String state,
      String postalCode,
      Double latitude,
      Double longitude,
      boolean isCustom,
      boolean isDefault,
      boolean isArchived,
      String hoursJson,
      long createdAt,
      long updatedAt) {
    this.id = requireId(id, "store id");
    this.brand = Objects.requireNonNull(brand, "brand must not be null");
    this.name = requireText(name, "store name");
    this.addressLine = TextNormalizer.trimToNull(addressLine);
    this.city = TextNormalizer.trimToNull(city);
    this.state = TextNormalizer.trimToNull(state);
    this.postalCode = TextNormalizer.trimToNull(postalCode);
    this.latitude = validateLatitude(latitude);
    this.longitude = validateLongitude(longitude);
    this.isCustom = isCustom;
    this.isDefault = isDefault;
    this.isArchived = isArchived;
    this.hoursJson = TextNormalizer.trimToNull(hoursJson);
    this.createdAt = requireTimestamp(createdAt, "createdAt");
    this.updatedAt = requireTimestamp(updatedAt, "updatedAt");
  }

  private static String requireId(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static String requireText(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  private static long requireTimestamp(long value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
    return value;
  }

  private static Double validateLatitude(Double value) {
    if (value != null && (value < -90.0 || value > 90.0)) {
      throw new IllegalArgumentException("latitude must be between -90 and 90");
    }
    return value;
  }

  private static Double validateLongitude(Double value) {
    if (value != null && (value < -180.0 || value > 180.0)) {
      throw new IllegalArgumentException("longitude must be between -180 and 180");
    }
    return value;
  }

  public String getId() {
    return id;
  }

  public StoreBrand getBrand() {
    return brand;
  }

  public String getName() {
    return name;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public boolean isCustom() {
    return isCustom;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public boolean isArchived() {
    return isArchived;
  }

  public String getHoursJson() {
    return hoursJson;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }
}
