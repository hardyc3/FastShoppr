/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.common;

import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Locale-aware date text and ISO parsing used by list, detail, edit, and review screens. */
public final class DateText {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private DateText() {}

  public static String formatIso(LocalDate date) {
    return date.format(ISO_DATE);
  }

  public static String formatMedium(LocalDate date) {
    return date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
  }

  @Nullable
  public static LocalDate parseIsoOrNull(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim(), ISO_DATE);
    } catch (RuntimeException exception) {
      return null;
    }
  }
}
