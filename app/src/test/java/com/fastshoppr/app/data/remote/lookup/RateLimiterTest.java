/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.testing.FixedClock;
import org.junit.Test;

public class RateLimiterTest {

  @Test
  public void enforcesAtLeastOneSecondPerStore() {
    FixedClock clock = new FixedClock(1_000L);
    RateLimiter limiter = new RateLimiter(clock);
    long start = System.nanoTime();
    limiter.acquire(StoreBrand.WALMART);
    clock.setNow(1_200L);
    limiter.acquire(StoreBrand.WALMART);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
    assertTrue("expected rate limit delay, elapsed=" + elapsedMs, elapsedMs >= 700L);
  }

  @Test
  public void doesNotDelayDifferentStores() {
    FixedClock clock = new FixedClock(1_000L);
    RateLimiter limiter = new RateLimiter(clock);
    limiter.acquire(StoreBrand.WALMART);
    clock.setNow(1_100L);
    long start = System.nanoTime();
    limiter.acquire(StoreBrand.HOME_DEPOT);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
    assertTrue(elapsedMs < 300L);
  }
}
