/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import static org.junit.Assert.assertEquals;

import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.List;
import org.junit.Test;

public class StoreLookupSupportTest {

  private final StoreLookupSupport support = new StoreLookupSupport();

  @Test
  public void walmartRemainsTheOnlyAutomatedLookupBrand() {
    assertEquals(StoreLookupSupport.Mode.ENABLED, support.forBrand(StoreBrand.WALMART).mode);
  }

  @Test
  public void blockedRetailersHaveNoAutomatedLookup() {
    for (StoreBrand brand : List.of(StoreBrand.HOME_DEPOT, StoreBrand.LOWES, StoreBrand.TARGET)) {
      assertEquals(StoreLookupSupport.Mode.BLOCKED, support.forBrand(brand).mode);
    }
  }

  @Test
  public void smithsHasNoPublicAisleOnProductPages() {
    assertEquals(StoreLookupSupport.Mode.NO_PUBLIC_AISLE, support.forBrand(StoreBrand.SMITHS).mode);
  }

  @Test
  public void customStoresStayManualOnly() {
    assertEquals(StoreLookupSupport.Mode.MANUAL_ONLY, support.forBrand(StoreBrand.CUSTOM).mode);
    assertEquals(StoreLookupSupport.Mode.MANUAL_ONLY, support.forBrand(null).mode);
  }
}
