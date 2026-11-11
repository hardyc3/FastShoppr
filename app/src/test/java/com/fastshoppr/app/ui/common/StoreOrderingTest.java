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
    Store target = store("t", StoreBrand.TARGET, "Target");
    Store walmartB = store("wb", StoreBrand.WALMART, "Walmart B");
    Store walmartA = store("wa", StoreBrand.WALMART, "Walmart A");
    List<Store> input = List.of(target, walmartB, walmartA);

    List<Store> ordered = StoreOrdering.byBrandThenName(input);

    assertEquals(List.of("wa", "wb", "t"), ordered.stream().map(Store::getId).toList());
    assertEquals("t", input.get(0).getId());
  }

  private static Store store(String id, StoreBrand brand, String name) {
    return new Store(
        id, brand, name, null, null, null, null, null, null, false, false, false, null, 1, 1);
  }
}
