/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Enforces at most one request per second per store brand. */
@Singleton
public class RateLimiter {

  private static final long MIN_INTERVAL_MILLIS = 1_000L;

  private final Clock clock;
  private final Map<StoreBrand, Long> nextSlotByBrand = new EnumMap<>(StoreBrand.class);

  @Inject
  public RateLimiter(Clock clock) {
    this.clock = clock;
  }

  /**
   * Reserves the next allowed slot for {@code brand} and blocks until that slot is reached. Slot
   * allocation is monotonic; the actual waiting happens outside the lock so a wait for one brand
   * never blocks another.
   */
  public void acquire(StoreBrand brand) {
    long waitMs;
    synchronized (nextSlotByBrand) {
      long now = clock.nowMillis();
      Long next = nextSlotByBrand.get(brand);
      long slot = (next == null || next < now) ? now : next;
      waitMs = slot - now;
      nextSlotByBrand.put(brand, slot + MIN_INTERVAL_MILLIS);
    }
    if (waitMs > 0) {
      try {
        Thread.sleep(waitMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while rate limiting", e);
      }
    }
  }
}
