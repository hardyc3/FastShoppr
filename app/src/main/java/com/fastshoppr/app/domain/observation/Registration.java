/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.observation;

/** Handle for an active observation. Closing it stops further deliveries. */
public interface Registration extends AutoCloseable {

  @Override
  void close();
}
