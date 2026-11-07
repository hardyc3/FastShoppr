/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.local;

import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.List;

public final class SeedStores {

  public static final String WALMART_ID = "0f8fad5b-d9cb-469f-a165-70867728950e";
  public static final String HOME_DEPOT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
  public static final String LOWES_ID = "a3a4e1b2-3c4d-4e5f-8a9b-0c1d2e3f4a5b";
  public static final String TARGET_ID = "b4b5e2c3-4d5e-4f6a-9b0c-1d2e3f4a5b6c";
  public static final String SMITHS_ID = "c5c6e3d4-5e6f-4a7b-8c1d-2e3f4a5b6c7d";

  private static final List<StoreBrand> BRANDS =
      List.of(
          StoreBrand.WALMART,
          StoreBrand.HOME_DEPOT,
          StoreBrand.LOWES,
          StoreBrand.TARGET,
          StoreBrand.SMITHS);

  private SeedStores() {}

  public static List<StoreBrand> brands() {
    return BRANDS;
  }

  public static String idFor(StoreBrand brand) {
    return switch (brand) {
      case WALMART -> WALMART_ID;
      case HOME_DEPOT -> HOME_DEPOT_ID;
      case LOWES -> LOWES_ID;
      case TARGET -> TARGET_ID;
      case SMITHS -> SMITHS_ID;
      case CUSTOM -> throw new IllegalArgumentException("Custom stores are not seeded");
    };
  }

  public static String nameFor(StoreBrand brand) {
    return switch (brand) {
      case WALMART -> "Walmart";
      case HOME_DEPOT -> "Home Depot";
      case LOWES -> "Lowe's";
      case TARGET -> "Target";
      case SMITHS -> "Smiths";
      case CUSTOM -> throw new IllegalArgumentException("Custom stores are not seeded");
    };
  }
}
