/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

/** Signals that a public store locator blocked a request. */
public final class StoreLocatorBlockedException extends RuntimeException {

  public StoreLocatorBlockedException(String message) {
    super(message);
  }
}
