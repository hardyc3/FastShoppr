/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.testing;

import com.fastshoppr.app.domain.time.Clock;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class FakeClock implements Clock {

  public static final long NOW = 1_767_225_600_000L;

  @Inject
  public FakeClock() {}

  @Override
  public long nowMillis() {
    return NOW;
  }
}
