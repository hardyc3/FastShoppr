/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.error;

public final class NotFoundException extends DomainException {

  public NotFoundException(String message) {
    super(message);
  }
}
