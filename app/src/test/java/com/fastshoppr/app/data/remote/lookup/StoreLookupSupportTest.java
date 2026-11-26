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
  public void automatedLookupBrandsAreEnabledAfterSearchFirstVerification() {
    for (StoreBrand brand :
        List.of(StoreBrand.WALMART, StoreBrand.HOME_DEPOT, StoreBrand.LOWES, StoreBrand.SMITHS)) {
      assertEquals(StoreLookupSupport.Mode.ENABLED, support.forBrand(brand).mode);
    }
  }

  @Test
  public void customStoresStayManualOnly() {
    assertEquals(StoreLookupSupport.Mode.MANUAL_ONLY, support.forBrand(StoreBrand.CUSTOM).mode);
    assertEquals(StoreLookupSupport.Mode.MANUAL_ONLY, support.forBrand(null).mode);
  }
}
