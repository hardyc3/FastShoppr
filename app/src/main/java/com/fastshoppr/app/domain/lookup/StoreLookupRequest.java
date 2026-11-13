/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.lookup;

import com.fastshoppr.app.domain.model.Store;
import java.util.Objects;

/** Immutable input for a single store lookup. */
public final class StoreLookupRequest {

  private final String rawText;
  private final String displayName;
  private final String normalizedText;
  private final Store store;

  public StoreLookupRequest(
      String rawText, String displayName, String normalizedText, Store store) {
    this.rawText = require(rawText, "rawText");
    this.displayName = require(displayName, "displayName");
    this.normalizedText = require(normalizedText, "normalizedText");
    this.store = Objects.requireNonNull(store, "store must not be null");
  }

  private static String require(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }

  public String getRawText() {
    return rawText;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getNormalizedText() {
    return normalizedText;
  }

  public Store getStore() {
    return store;
  }
}
