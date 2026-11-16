/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import com.fastshoppr.app.domain.model.ShoppingListSummary;
import java.time.LocalDate;

public final class CalendarDay {
  public final LocalDate date; // null for padding
  public final boolean inMonth;
  public final boolean isToday;
  public final ShoppingListSummary summary; // null if none

  public CalendarDay(LocalDate date, boolean inMonth, boolean isToday, ShoppingListSummary summary) {
    this.date = date;
    this.inMonth = inMonth;
    this.isToday = isToday;
    this.summary = summary;
  }
}
