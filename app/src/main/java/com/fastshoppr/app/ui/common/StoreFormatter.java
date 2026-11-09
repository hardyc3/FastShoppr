/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import com.fastshoppr.app.domain.model.Store;
import java.util.ArrayList;
import java.util.List;

public final class StoreFormatter {

  private StoreFormatter() {}

  public static String formatAddress(Store store) {
    List<String> parts = new ArrayList<>();
    addIfPresent(parts, store.getAddressLine());
    addIfPresent(parts, store.getCity());
    addIfPresent(parts, store.getState());
    addIfPresent(parts, store.getPostalCode());
    return parts.isEmpty() ? null : String.join(", ", parts);
  }

  private static void addIfPresent(List<String> parts, String value) {
    if (value != null && !value.trim().isEmpty()) {
      parts.add(value.trim());
    }
  }
}
