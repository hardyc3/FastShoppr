/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.error;

public final class StoreInUseException extends DomainException {

  public StoreInUseException(String message) {
    super(message);
  }
}
