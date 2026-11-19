/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class NearbyStoreRankingTest {

  @Test
  public void sortsByDistanceAndPutsUnknownCoordinatesLast() {
    GeoPoint origin = new GeoPoint(39.78, -89.64);
    NearbyStore far = store("far", 40.0, -89.0);
    NearbyStore unknown = store("unknown", null, null);
    NearbyStore near = store("near", 39.8, -89.6);

    List<NearbyStore> ranked =
        NearbyStoreRanking.nearestFirst(List.of(far, unknown, near), origin, 8);

    assertEquals("near", ranked.get(0).getName());
    assertEquals("far", ranked.get(1).getName());
    assertEquals("unknown", ranked.get(2).getName());
    assertTrue(NearbyStoreRanking.distanceMiles(origin, near) < 20.0);
  }

  @Test
  public void limitsResultsAndPreservesOrderWithoutOrigin() {
    NearbyStore first = store("first", 10.0, 10.0);
    NearbyStore second = store("second", 20.0, 20.0);
    NearbyStore third = store("third", 30.0, 30.0);

    List<NearbyStore> ranked =
        NearbyStoreRanking.nearestFirst(List.of(first, second, third), null, 2);

    assertEquals(List.of(first, second), ranked);
  }

  @Test
  public void cachedResultsKeepTheirNearestFirstOrder() {
    GeoPoint origin = new GeoPoint(39.78, -89.64);
    NearbyStore nearest = store("nearest", null, null);
    NearbyStore second = store("second", null, null);

    List<NearbyStore> ranked = NearbyStoreRanking.nearestFirst(List.of(nearest, second), origin, 8);

    assertEquals(List.of(nearest, second), ranked);
  }

  private static NearbyStore store(String name, Double latitude, Double longitude) {
    return new NearbyStore(
        name,
        StoreBrand.WALMART,
        name,
        null,
        null,
        null,
        null,
        latitude,
        longitude,
        NearbyStoreSource.LOCATOR);
  }
}
