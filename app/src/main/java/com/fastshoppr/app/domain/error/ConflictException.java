/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.error;

public final class ConflictException extends DomainException {

  public ConflictException(String message) {
    super(message);
  }
}
