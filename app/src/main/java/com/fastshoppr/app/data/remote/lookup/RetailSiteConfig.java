/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.StoreBrand;

/** Immutable per-retailer configuration for the shared search-first HTTP lookup. */
public final class RetailSiteConfig {

  public static final RetailSiteConfig HOME_DEPOT =
      new RetailSiteConfig(
          StoreBrand.HOME_DEPOT,
          "https://www.homedepot.com/s/",
          "homedepot.com",
          "/p/",
          LookupSource.HOME_DEPOT_HTTP);
  public static final RetailSiteConfig LOWES =
      new RetailSiteConfig(
          StoreBrand.LOWES,
          "https://www.lowes.com/search?searchTerm=",
          "lowes.com",
          "/pd/",
          LookupSource.LOWES_HTTP);
  public static final RetailSiteConfig SMITHS =
      new RetailSiteConfig(
          StoreBrand.SMITHS,
          "https://www.smithsfoodanddrug.com/search?searchTerm=",
          "smithsfoodanddrug.com",
          "/p/",
          LookupSource.SMITHS_HTTP);

  public final StoreBrand brand;
  public final String searchBaseUrl;
  public final String host;
  public final String productPathToken;
  public final LookupSource source;

  private RetailSiteConfig(
      StoreBrand brand,
      String searchBaseUrl,
      String host,
      String productPathToken,
      LookupSource source) {
    this.brand = brand;
    this.searchBaseUrl = searchBaseUrl;
    this.host = host;
    this.productPathToken = productPathToken;
    this.source = source;
  }
}
