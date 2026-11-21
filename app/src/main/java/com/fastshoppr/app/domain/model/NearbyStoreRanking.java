/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Sorts locator results by distance from the user's location. */
public final class NearbyStoreRanking {

  private static final double EARTH_RADIUS_MILES = 3_958.8;
  private static final double UNKNOWN_DISTANCE = Double.MAX_VALUE;

  private NearbyStoreRanking() {}

  public static List<NearbyStore> nearestFirst(
      List<NearbyStore> stores, GeoPoint origin, int limit) {
    List<NearbyStore> sorted = new ArrayList<>(stores);
    // Cached candidates deliberately omit coordinates, so keep their already-ranked order
    // instead of replacing it with an alphabetical sort.
    boolean anyLocated = sorted.stream().anyMatch(NearbyStore::hasCoordinates);
    if (origin != null && anyLocated) {
      sorted.sort(
          Comparator.comparingDouble((NearbyStore store) -> distanceMiles(origin, store))
              .thenComparing(NearbyStore::getName, String.CASE_INSENSITIVE_ORDER));
    }
    if (limit <= 0 || sorted.size() <= limit) {
      return List.copyOf(sorted);
    }
    return List.copyOf(sorted.subList(0, limit));
  }

  public static double distanceMiles(GeoPoint origin, NearbyStore store) {
    if (origin == null || !store.hasCoordinates()) {
      return UNKNOWN_DISTANCE;
    }
    double lat1 = Math.toRadians(origin.getLatitude());
    double lat2 = Math.toRadians(store.getLatitude());
    double deltaLat = lat2 - lat1;
    double deltaLon = Math.toRadians(store.getLongitude() - origin.getLongitude());
    double a =
        Math.sin(deltaLat / 2.0) * Math.sin(deltaLat / 2.0)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2.0) * Math.sin(deltaLon / 2.0);
    a = Math.max(0.0, Math.min(1.0, a));
    return EARTH_RADIUS_MILES * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
  }
}
