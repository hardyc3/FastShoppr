/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import org.junit.Test;

public final class DateTextTest {

  @Test
  public void parsesTrimmedIsoDate() {
    assertEquals(LocalDate.of(2026, 11, 8), DateText.parseIsoOrNull(" 2026-11-08 "));
  }

  @Test
  public void invalidOrMissingDateReturnsNull() {
    assertNull(DateText.parseIsoOrNull(null));
    assertNull(DateText.parseIsoOrNull(""));
    assertNull(DateText.parseIsoOrNull("not-a-date"));
  }

  @Test
  public void formatsIso() {
    assertEquals("2026-11-08", DateText.formatIso(LocalDate.of(2026, 11, 8)));
  }
}
