/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.time;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SystemClockTest {

  @Test
  public void nowMillisReturnsCurrentEpochTime() {
    long before = System.currentTimeMillis();

    long now = new SystemClock().nowMillis();

    long after = System.currentTimeMillis();
    assertTrue(now >= before);
    assertTrue(now <= after);
  }
}
