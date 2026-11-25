/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.testing.FixedClock;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class RateLimiterTest {

  private static final class RecordingSleeper implements RateLimiter.Sleeper {
    final List<Long> waits = new ArrayList<>();

    @Override
    public void sleep(long millis) {
      waits.add(millis);
    }
  }

  @Test
  public void enforcesAtLeastOneSecondPerStoreWithoutWallClock() {
    FixedClock clock = new FixedClock(1_000L);
    RecordingSleeper sleeper = new RecordingSleeper();
    RateLimiter limiter = new RateLimiter(clock, sleeper);

    limiter.acquire(StoreBrand.WALMART);
    clock.setNow(1_200L);
    limiter.acquire(StoreBrand.WALMART);
    clock.setNow(2_100L);
    limiter.acquire(StoreBrand.WALMART);

    assertEquals(List.of(800L, 900L), sleeper.waits);
  }

  @Test
  public void doesNotDelayDifferentStores() {
    FixedClock clock = new FixedClock(1_000L);
    RecordingSleeper sleeper = new RecordingSleeper();
    RateLimiter limiter = new RateLimiter(clock, sleeper);

    limiter.acquire(StoreBrand.WALMART);
    clock.setNow(1_100L);
    limiter.acquire(StoreBrand.HOME_DEPOT);

    assertTrue(sleeper.waits.isEmpty());
  }
}
