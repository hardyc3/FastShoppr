/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

public enum StoreBrand {
  WALMART("Walmart"),
  HOME_DEPOT("Home Depot"),
  LOWES("Lowe's"),
  SMITHS("Smiths"),
  CUSTOM("Custom");

  private final String displayName;

  StoreBrand(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
