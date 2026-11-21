/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.fastshoppr.app.domain.model.GeoPoint;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.StoreDirectoryRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;

public final class FindNearbyStoresUseCaseTest {

  @Test
  public void ranksResultsNearestFirst() throws Exception {
    NearbyStore far = store("far", 45.0, -90.0);
    NearbyStore near = store("near", 39.8, -89.6);
    StoreDirectoryRepository repository =
        (brand, origin, postalCode) -> CompletableFuture.completedFuture(List.of(far, near));

    List<NearbyStore> results =
        new FindNearbyStoresUseCase(repository)
            .execute(StoreBrand.WALMART, new GeoPoint(39.78, -89.64), null)
            .get();

    assertEquals(List.of(near, far), results);
  }

  @Test
  public void rejectsCustomBrandAndEmptyLocation() {
    FindNearbyStoresUseCase useCase =
        new FindNearbyStoresUseCase(
            (brand, origin, postalCode) -> CompletableFuture.completedFuture(List.of()));

    assertThrows(
        IllegalArgumentException.class,
        () -> useCase.execute(StoreBrand.CUSTOM, new GeoPoint(0, 0), null));
    assertThrows(
        IllegalArgumentException.class, () -> useCase.execute(StoreBrand.WALMART, null, "  "));
  }

  private static NearbyStore store(String name, double latitude, double longitude) {
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
