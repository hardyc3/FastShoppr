/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.time;

import com.fastshoppr.app.domain.time.Clock;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SystemClock implements Clock {

  @Inject
  public SystemClock() {}

  @Override
  public long nowMillis() {
    return System.currentTimeMillis();
  }
}
