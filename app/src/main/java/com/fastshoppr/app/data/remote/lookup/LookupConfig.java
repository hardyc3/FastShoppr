/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Runtime gates for store lookup integrations.
 *
 * <p>Per D-055, Home Depot, Lowe's, and Smiths use the shared search-first HTTP path. Walmart
 * lookups remain user-initiated and foreground-only; this config only gates automated integrations.
 */
@Singleton
public class LookupConfig {

  @Inject
  public LookupConfig() {}

  /** The search-first retail path is enabled; 403/429 and challenge pages still stop the run. */
  public boolean isRetailHttpEnabled() {
    return true;
  }
}
