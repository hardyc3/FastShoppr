/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import com.fastshoppr.app.domain.model.Store;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shared presentation order for store selectors and list sections. */
public final class StoreOrdering {

  private static final Comparator<Store> BY_BRAND_THEN_NAME =
      Comparator.comparingInt((Store store) -> store.getBrand().ordinal())
          .thenComparing(Store::getName, String.CASE_INSENSITIVE_ORDER);

  private StoreOrdering() {}

  public static List<Store> byBrandThenName(List<Store> stores) {
    List<Store> ordered = new ArrayList<>(stores);
    ordered.sort(BY_BRAND_THEN_NAME);
    return List.copyOf(ordered);
  }
}
