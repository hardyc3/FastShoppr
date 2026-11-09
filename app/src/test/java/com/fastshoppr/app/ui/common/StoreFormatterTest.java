/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import org.junit.Test;

public final class StoreFormatterTest {

  @Test
  public void formatsAddressFromParts() {
    Store store =
        new Store(
            "store-1",
            StoreBrand.WALMART,
            "Walmart",
            "123 Main St",
            "Springfield",
            "IL",
            "62704",
            40.0,
            -89.0,
            false,
            false,
            false,
            null,
            0,
            0);

    assertEquals("123 Main St, Springfield, IL, 62704", StoreFormatter.formatAddress(store));
  }

  @Test
  public void returnsNullWhenAddressMissing() {
    Store store =
        new Store(
            "store-2",
            StoreBrand.WALMART,
            "Walmart",
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            false,
            null,
            0,
            0);

    assertNull(StoreFormatter.formatAddress(store));
  }
}
