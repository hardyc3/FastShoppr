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
 * <p>Per D-021/D-022, Home Depot live HTTP lookup is dormant until a real-device pass confirms
 * aisle pages are reachable. Walmart lookups must remain user-initiated and foreground-only; this
 * config only gates automated/network integrations.
 */
@Singleton
public class LookupConfig {

  @Inject
  public LookupConfig() {}

  /** Home Depot is blocked (403) in current environment; keep HTTP path dormant by default. */
  public boolean isHomeDepotLiveEnabled() {
    return false;
  }
}
