/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.repository;

import com.fastshoppr.app.domain.model.GeoPoint;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Looks up physical branches from a retailer's public store locator page. */
public interface StoreDirectoryRepository {

  /**
   * Finds branches near {@code origin} (preferred) or matching {@code postalCode}. At least one
   * argument must be non-null.
   */
  CompletableFuture<List<NearbyStore>> findNearby(
      StoreBrand brand, GeoPoint origin, String postalCode);
}
