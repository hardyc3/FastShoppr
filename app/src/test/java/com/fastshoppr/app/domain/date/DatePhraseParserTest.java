/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.Test;

public final class DatePhraseParserTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
  private static final LocalDate LIST_DATE = LocalDate.of(2026, 11, 5);

  @Test
  public void parsesTomorrowRelativeToToday() {
    Optional<DatePhraseParser.ParsedDate> opt =
        DatePhraseParser.parse("batteries tomorrow", LIST_DATE, TODAY);
    assertTrue(opt.isPresent());
    DatePhraseParser.ParsedDate p = opt.get();
    assertEquals("batteries", p.itemText());
    assertEquals(TODAY.plusDays(1), p.resolvedDate().orElseThrow());
    assertEquals(DatePhraseParser.Anchor.TODAY, p.anchor());
    assertFalse(p.needsConfirmation());
  }

  @Test
  public void parsesWaitAWeekRelativeToListDate() {
    DatePhraseParser.ParsedDate p =
        DatePhraseParser.parse("toilet paper wait a week", LIST_DATE, TODAY).orElseThrow();
    assertEquals("toilet paper", p.itemText());
    assertEquals(LIST_DATE.plusWeeks(1), p.resolvedDate().orElseThrow());
    assertEquals(DatePhraseParser.Anchor.CURRENT_LIST_DATE, p.anchor());
  }

  @Test
  public void parsesNextWeekInNDaysAndInNWeeks() {
    assertEquals(
        LIST_DATE.plusWeeks(1),
        DatePhraseParser.parse("milk next week", LIST_DATE, TODAY)
            .orElseThrow()
            .resolvedDate()
            .orElseThrow());
    assertEquals(
        LIST_DATE.plusDays(3),
        DatePhraseParser.parse("paper towels in 3 days", LIST_DATE, TODAY)
            .orElseThrow()
            .resolvedDate()
            .orElseThrow());
    assertEquals(
        LIST_DATE.plusWeeks(2),
        DatePhraseParser.parse("eggs in 2 weeks", LIST_DATE, TODAY)
            .orElseThrow()
            .resolvedDate()
            .orElseThrow());
  }

  @Test
  public void parsesNextMonthThisWeekendAndNextShoppingDay() {
    assertEquals(
        LIST_DATE.plusMonths(1),
        DatePhraseParser.parse("light bulbs next month", LIST_DATE, TODAY)
            .orElseThrow()
            .resolvedDate()
            .orElseThrow());
    assertEquals(
        LocalDate.of(2026, 11, 7),
        DatePhraseParser.parse("bread this weekend", LIST_DATE, TODAY)
            .orElseThrow()
            .resolvedDate()
            .orElseThrow());
    DatePhraseParser.ParsedDate nsd =
        DatePhraseParser.parse("coffee next shopping day", LIST_DATE, TODAY).orElseThrow();
    assertTrue(nsd.requiresSchedule());
    assertFalse(nsd.resolvedDate().isPresent());
  }

  @Test
  public void ignoresNonDateAndPhraseOnly() {
    assertFalse(DatePhraseParser.parse("3 day deodorant", LIST_DATE, TODAY).isPresent());
    assertFalse(DatePhraseParser.parse("paper towels", LIST_DATE, TODAY).isPresent());
    assertFalse(DatePhraseParser.parse("tomorrow", LIST_DATE, TODAY).isPresent());
  }

  @Test
  public void flagsPastResolvedDateForConfirmation() {
    DatePhraseParser.ParsedDate p =
        DatePhraseParser.parse(
                "milk next week", LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 10))
            .orElseThrow();
    assertTrue(p.needsConfirmation());
  }

  @Test
  public void handlesMonthEndAndLeapYear() {
    assertEquals(
        LocalDate.of(2026, 2, 28),
        DatePhraseParser.parse("x next month", LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 1))
            .orElseThrow()
            .resolvedDate()
            .orElseThrow());
    assertEquals(
        LocalDate.of(2028, 2, 29),
        DatePhraseParser.parse("x next month", LocalDate.of(2028, 1, 31), LocalDate.of(2028, 1, 1))
            .orElseThrow()
            .resolvedDate()
            .orElseThrow());
  }
}
