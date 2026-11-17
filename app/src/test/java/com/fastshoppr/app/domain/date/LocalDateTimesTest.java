/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.date;

import static org.junit.Assert.assertEquals;

import com.fastshoppr.app.testing.FixedClock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.Test;

public final class LocalDateTimesTest {

  private static final ZoneId MOUNTAIN = ZoneId.of("America/Denver");

  @Test
  public void keepsCalendarDateAcrossSpringForward() {
    assertEquals(
        LocalDate.of(2026, 3, 7), LocalDateTimes.today(clockAt("2026-03-08T06:30:00Z"), MOUNTAIN));
    assertEquals(
        LocalDate.of(2026, 3, 8), LocalDateTimes.today(clockAt("2026-03-08T08:00:00Z"), MOUNTAIN));
    assertEquals(
        LocalDate.of(2026, 3, 8), LocalDateTimes.today(clockAt("2026-03-08T09:00:00Z"), MOUNTAIN));
  }

  @Test
  public void keepsCalendarDateAcrossFallBackRepeatedHour() {
    assertEquals(
        LocalDate.of(2026, 11, 1), LocalDateTimes.today(clockAt("2026-11-01T07:30:00Z"), MOUNTAIN));
    assertEquals(
        LocalDate.of(2026, 11, 1), LocalDateTimes.today(clockAt("2026-11-01T09:30:00Z"), MOUNTAIN));
    assertEquals(
        LocalDate.of(2026, 11, 2), LocalDateTimes.today(clockAt("2026-11-02T07:30:00Z"), MOUNTAIN));
  }

  @Test
  public void resolvesDateInRequestedTimeZone() {
    FixedClock west = clockAt("2026-01-01T09:00:00Z");
    assertEquals(LocalDate.of(2026, 1, 1), LocalDateTimes.today(west, ZoneId.of("UTC")));
    assertEquals(
        LocalDate.of(2025, 12, 31), LocalDateTimes.today(west, ZoneId.of("Pacific/Honolulu")));

    FixedClock east = clockAt("2026-01-01T10:00:00Z");
    assertEquals(LocalDate.of(2026, 1, 1), LocalDateTimes.today(east, ZoneId.of("UTC")));
    assertEquals(
        LocalDate.of(2026, 1, 2), LocalDateTimes.today(east, ZoneId.of("Pacific/Kiritimati")));
  }

  private static FixedClock clockAt(String instant) {
    return new FixedClock(Instant.parse(instant).toEpochMilli());
  }
}
