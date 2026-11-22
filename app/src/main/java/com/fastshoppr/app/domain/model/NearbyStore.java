/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import com.fastshoppr.app.domain.text.TextNormalizer;
import java.util.Objects;

/** A physical store branch returned by a retailer locator or entered by the user. */
public final class NearbyStore {

  private final String id;
  private final StoreBrand brand;
  private final String name;
  private final String addressLine;
  private final String city;
  private final String state;
  private final String postalCode;
  // Transient so Gson never persists device or branch coordinates.
  private final transient Double latitude;
  private final transient Double longitude;
  private final NearbyStoreSource source;

  public NearbyStore(
      String id,
      StoreBrand brand,
      String name,
      String addressLine,
      String city,
      String state,
      String postalCode,
      Double latitude,
      Double longitude,
      NearbyStoreSource source) {
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("store id must not be blank");
    }
    this.id = id.trim();
    this.brand = Objects.requireNonNull(brand, "brand must not be null");
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("store name must not be blank");
    }
    this.name = name.trim();
    this.addressLine = TextNormalizer.trimToNull(addressLine);
    this.city = TextNormalizer.trimToNull(city);
    this.state = TextNormalizer.trimToNull(state);
    this.postalCode = TextNormalizer.trimToNull(postalCode);
    if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
      throw new IllegalArgumentException("latitude must be between -90 and 90");
    }
    if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
      throw new IllegalArgumentException("longitude must be between -180 and 180");
    }
    this.latitude = latitude;
    this.longitude = longitude;
    this.source = Objects.requireNonNull(source, "source must not be null");
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

  public NearbyStoreSource getSource() {
    return source;
  }

  public boolean hasCoordinates() {
    return latitude != null && longitude != null;
  }

  /** Human-readable label that never includes coordinates. */
  public String getLabel() {
    StringBuilder label = new StringBuilder(name);
    String address = getAddressLine();
    if (address != null && !address.isBlank()) {
      label.append(" — ").append(address);
    }
    if (city != null && !city.isBlank()) {
      label.append(", ").append(city);
    }
    if (state != null && !state.isBlank()) {
      label.append(", ").append(state);
    }
    return label.toString();
  }
}
