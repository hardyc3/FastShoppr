/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.date;

import com.fastshoppr.app.domain.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Converts clock instants to the calendar date used by schedule and list logic. */
public final class LocalDateTimes {

  public static LocalDate today(Clock clock) {
    return today(clock, ZoneId.systemDefault());
  }

  public static LocalDate today(Clock clock, ZoneId zone) {
    return Instant.ofEpochMilli(clock.nowMillis()).atZone(zone).toLocalDate();
  }

  private LocalDateTimes() {}
}
