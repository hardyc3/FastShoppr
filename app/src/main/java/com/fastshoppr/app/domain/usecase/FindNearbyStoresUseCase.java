/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.GeoPoint;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreRanking;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.StoreDirectoryRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Finds the closest branches of one brand for the user to choose from. */
public final class FindNearbyStoresUseCase {

  private static final int MAX_RESULTS = 8;

  private final StoreDirectoryRepository repository;

  @Inject
  public FindNearbyStoresUseCase(StoreDirectoryRepository repository) {
    this.repository = repository;
  }

  public CompletableFuture<List<NearbyStore>> execute(
      StoreBrand brand, GeoPoint origin, String postalCode) {
    if (brand == null || brand == StoreBrand.CUSTOM) {
      throw new IllegalArgumentException("A supported brand is required");
    }
    if (origin == null && (postalCode == null || postalCode.isBlank())) {
      throw new IllegalArgumentException("A location or postal code is required");
    }
    return repository
        .findNearby(brand, origin, postalCode)
        .thenApply(stores -> NearbyStoreRanking.nearestFirst(stores, origin, MAX_RESULTS));
  }
}
