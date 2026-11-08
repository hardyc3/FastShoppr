/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.util.Objects;

public final class ShoppingListSummary {

  private final ShoppingList list;
  private final int itemCount;
  private final int storeCount;

  public ShoppingListSummary(ShoppingList list, int itemCount, int storeCount) {
    this.list = Objects.requireNonNull(list, "list must not be null");
    if (itemCount < 0) {
      throw new IllegalArgumentException("itemCount must not be negative");
    }
    if (storeCount < 0) {
      throw new IllegalArgumentException("storeCount must not be negative");
    }
    this.itemCount = itemCount;
    this.storeCount = storeCount;
  }

  public ShoppingList getList() {
    return list;
  }

  public int getItemCount() {
    return itemCount;
  }

  public int getStoreCount() {
    return storeCount;
  }
}
