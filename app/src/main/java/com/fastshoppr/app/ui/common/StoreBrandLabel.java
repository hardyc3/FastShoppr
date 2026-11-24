/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorPages;
import java.util.EnumMap;
import java.util.Map;

/** Maps domain brands to user-facing labels without depending on data-layer seed code. */
public final class StoreBrandLabel {

  private static final Map<StoreBrand, Integer> LABEL_RES = new EnumMap<>(StoreBrand.class);

  static {
    LABEL_RES.put(StoreBrand.WALMART, R.string.brand_walmart);
    LABEL_RES.put(StoreBrand.HOME_DEPOT, R.string.brand_home_depot);
    LABEL_RES.put(StoreBrand.LOWES, R.string.brand_lowes);
    LABEL_RES.put(StoreBrand.TARGET, R.string.brand_target);
    LABEL_RES.put(StoreBrand.SMITHS, R.string.brand_smiths);
    LABEL_RES.put(StoreBrand.CUSTOM, R.string.add_store_hint);
  }

  private StoreBrandLabel() {}

  public static int labelRes(StoreBrand brand) {
    Integer label = LABEL_RES.get(brand);
    if (label == null) {
      throw new IllegalArgumentException("Unknown store brand");
    }
    return label;
  }

  /** Order must match the {@code store_brands} string-array. */
  public static StoreBrand[] locatorBrands() {
    return StoreLocatorPages.brandOrder().toArray(new StoreBrand[0]);
  }
}
