/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local.entity;

import androidx.room.Embedded;

public final class ShoppingListSummaryEntity {

  @Embedded public final ShoppingListEntity list;

  public final int itemCount;

  public final int storeCount;

  public ShoppingListSummaryEntity(ShoppingListEntity list, int itemCount, int storeCount) {
    this.list = list;
    this.itemCount = itemCount;
    this.storeCount = storeCount;
  }
}
