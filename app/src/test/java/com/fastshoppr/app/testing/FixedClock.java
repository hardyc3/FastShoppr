/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.testing;

import com.fastshoppr.app.domain.time.Clock;

public final class FixedClock implements Clock {

  private long now;

  public FixedClock(long now) {
    this.now = now;
  }

  public void setNow(long now) {
    this.now = now;
  }

  public void advance(long millis) {
    now += millis;
  }

  @Override
  public long nowMillis() {
    return now;
  }
}
