/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

public enum StoreBrand {
  WALMART,
  HOME_DEPOT,
  LOWES,
  TARGET,
  SMITHS,
  MACYS,
  CUSTOM;

  public String getDisplayName() {
    return switch (this) {
      case WALMART -> "Walmart";
      case HOME_DEPOT -> "Home Depot";
      case LOWES -> "Lowe's";
      case TARGET -> "Target";
      case SMITHS -> "Smiths";
      case MACYS -> "Macy's";
      case CUSTOM -> "Custom";
    };
  }
}
