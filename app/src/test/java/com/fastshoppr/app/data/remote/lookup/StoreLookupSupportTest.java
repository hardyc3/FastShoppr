/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.List;
import org.junit.Test;

public class StoreLookupSupportTest {

  private final StoreLookupSupport support = new StoreLookupSupport();

  @Test
  public void walmartRemainsTheOnlyAutomatedLookupBrand() {
    StoreLookupSupport.Decision decision = support.forBrand(StoreBrand.WALMART);

    assertEquals(StoreLookupSupport.Mode.ENABLED, decision.mode);
    assertNull(decision.reason);
  }

  @Test
  public void blockedRetailersUseTheirObservedBlockReason() {
    assertEquals(
        "Home Depot access is blocked (HTTP 403); enter aisle manually.",
        support.forBrand(StoreBrand.HOME_DEPOT).reason);
    assertEquals(
        "Lowe's access is denied; enter aisle manually.",
        support.forBrand(StoreBrand.LOWES).reason);
    assertEquals(
        "Target requires human verification; enter aisle manually.",
        support.forBrand(StoreBrand.TARGET).reason);

    for (StoreBrand brand : List.of(StoreBrand.HOME_DEPOT, StoreBrand.LOWES, StoreBrand.TARGET)) {
      assertEquals(StoreLookupSupport.Mode.BLOCKED, support.forBrand(brand).mode);
      assertTrue(support.forBrand(brand).reason.contains("enter aisle manually"));
    }
  }

  @Test
  public void smithsHasNoPublicAisleOnProductPages() {
    StoreLookupSupport.Decision decision = support.forBrand(StoreBrand.SMITHS);

    assertEquals(StoreLookupSupport.Mode.NO_PUBLIC_AISLE, decision.mode);
    assertEquals(
        "Smiths product pages do not show a public aisle; enter aisle manually.", decision.reason);
  }

  @Test
  public void customStoresStayManualOnly() {
    StoreLookupSupport.Decision decision = support.forBrand(StoreBrand.CUSTOM);

    assertEquals(StoreLookupSupport.Mode.MANUAL_ONLY, decision.mode);
    assertEquals("Custom stores use manual aisle entry.", decision.reason);
  }
}
