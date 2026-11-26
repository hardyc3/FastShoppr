/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import static org.junit.Assert.assertEquals;

import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.List;
import org.junit.Test;

public final class StoreOrderingTest {

  @Test
  public void ordersByBrandThenNameWithoutMutatingInput() {
    Store smiths = store("s", StoreBrand.SMITHS, "Smiths");
    Store walmartB = store("wb", StoreBrand.WALMART, "Walmart B");
    Store walmartA = store("wa", StoreBrand.WALMART, "Walmart A");
    List<Store> input = List.of(smiths, walmartB, walmartA);

    List<Store> ordered = StoreOrdering.byBrandThenName(input);

    assertEquals(List.of("wa", "wb", "s"), ordered.stream().map(Store::getId).toList());
    assertEquals(List.of("s", "wb", "wa"), input.stream().map(Store::getId).toList());
  }

  private static Store store(String id, StoreBrand brand, String name) {
    return new Store(
        id, brand, name, null, null, null, null, null, null, false, false, false, 1, 1);
  }
}
