/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import com.fastshoppr.app.domain.model.StoreBrand;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Verified Phase 10 capability for each supported brand.
 *
 * <p>Live checks on 2026-11-25 found Home Depot blocked by HTTP 403, Lowe's blocked by Akamai,
 * Target behind human verification, and Smiths showing no public aisle on its product page. Those
 * brands stay manual-aisle-first; Walmart keeps its foreground WebView path. See D-053 and D-054.
 */
@Singleton
public final class StoreLookupSupport {

  public enum Mode {
    ENABLED,
    BLOCKED,
    NO_PUBLIC_AISLE,
    MANUAL_ONLY
  }

  public static final class Decision {
    public final Mode mode;

    private Decision(Mode mode) {
      this.mode = mode;
    }
  }

  @Inject
  public StoreLookupSupport() {}

  public Decision forBrand(StoreBrand brand) {
    if (brand == null) {
      return new Decision(Mode.MANUAL_ONLY);
    }
    return switch (brand) {
      case WALMART -> new Decision(Mode.ENABLED);
      case HOME_DEPOT, LOWES, TARGET -> new Decision(Mode.BLOCKED);
      case SMITHS -> new Decision(Mode.NO_PUBLIC_AISLE);
      case CUSTOM -> new Decision(Mode.MANUAL_ONLY);
    };
  }
}
