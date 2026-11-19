/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.StoreBrand;

/** Maps domain brands to user-facing labels without depending on data-layer seed code. */
public final class StoreBrandLabel {

  private StoreBrandLabel() {}

  public static int labelRes(StoreBrand brand) {
    return switch (brand) {
      case WALMART -> R.string.brand_walmart;
      case HOME_DEPOT -> R.string.brand_home_depot;
      case LOWES -> R.string.brand_lowes;
      case TARGET -> R.string.brand_target;
      case SMITHS -> R.string.brand_smiths;
      case MACYS -> R.string.brand_macys;
      case CUSTOM -> R.string.add_store_hint;
    };
  }

  /** Order must match the {@code store_brands} string-array. */
  public static StoreBrand[] locatorBrands() {
    return new StoreBrand[] {
      StoreBrand.WALMART,
      StoreBrand.HOME_DEPOT,
      StoreBrand.LOWES,
      StoreBrand.TARGET,
      StoreBrand.SMITHS,
      StoreBrand.MACYS
    };
  }
}
