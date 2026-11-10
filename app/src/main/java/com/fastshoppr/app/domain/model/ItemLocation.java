/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import com.fastshoppr.app.domain.text.TextNormalizer;

/**
 * Cleaned aisle/bay pair and the lookup state that a manually supplied location implies.
 *
 * <p>Keeping this rule in one place stops the item editor and the aisle action from drifting apart.
 */
public record ItemLocation(String aisle, String bay) {

  public static ItemLocation of(String aisle, String bay) {
    return new ItemLocation(TextNormalizer.trimToNull(aisle), TextNormalizer.trimToNull(bay));
  }

  public boolean isPresent() {
    return aisle != null || bay != null;
  }

  public LookupStatus lookupStatus() {
    return isPresent() ? LookupStatus.FOUND : LookupStatus.PENDING;
  }

  public LookupSource lookupSource() {
    return isPresent() ? LookupSource.MANUAL : null;
  }
}
