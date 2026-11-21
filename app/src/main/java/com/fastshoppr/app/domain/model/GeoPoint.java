/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

/** A latitude/longitude pair. Coordinates are never logged or persisted by domain code. */
public final class GeoPoint {

  private final double latitude;
  private final double longitude;

  public GeoPoint(double latitude, double longitude) {
    if (latitude < -90.0 || latitude > 90.0) {
      throw new IllegalArgumentException("latitude must be between -90 and 90");
    }
    if (longitude < -180.0 || longitude > 180.0) {
      throw new IllegalArgumentException("longitude must be between -180 and 180");
    }
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }
}
