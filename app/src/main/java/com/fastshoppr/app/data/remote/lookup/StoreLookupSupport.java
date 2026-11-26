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
 * Verified lookup capability for each supported brand.
 *
 * <p>Walmart keeps its foreground WebView path. Home Depot, Lowe's, and Smiths use the shared
 * search-first HTTP path: search the public site, open the first matching product, then parse the
 * visible aisle. Target was removed after live checks showed no public aisle (D-056). Custom stores
 * remain manual-only. See D-053 through D-055.
 */
@Singleton
public final class StoreLookupSupport {

  public enum Mode {
    ENABLED,
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
      case WALMART, HOME_DEPOT, LOWES, SMITHS -> new Decision(Mode.ENABLED);
      case CUSTOM -> new Decision(Mode.MANUAL_ONLY);
    };
  }
}
