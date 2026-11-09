/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.LocalDate;
import org.junit.Test;

public final class ShoppingScheduleTest {

  @Test
  public void rejectsIntervalBelowOne() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ShoppingSchedule(
                "id", 0, IntervalUnit.DAYS, LocalDate.of(2026, 11, 7), true, true, null, 1, 1));
  }

  @Test
  public void storesScheduleValues() {
    ShoppingSchedule schedule =
        new ShoppingSchedule(
            "id", 2, IntervalUnit.WEEKS, LocalDate.of(2026, 11, 7), true, false, null, 1, 2);

    assertEquals(2, schedule.getIntervalValue());
    assertEquals(IntervalUnit.WEEKS, schedule.getIntervalUnit());
  }
}
