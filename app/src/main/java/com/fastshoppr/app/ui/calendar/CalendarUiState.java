/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import com.fastshoppr.app.domain.model.ShoppingListSummary;
import java.time.YearMonth;
import java.util.List;

public final class CalendarUiState {
  public final YearMonth month;
  public final List<CalendarDay> days;
  public final List<ShoppingListSummary> upcoming;

  public CalendarUiState(
      YearMonth month, List<CalendarDay> days, List<ShoppingListSummary> upcoming) {
    this.month = month;
    this.days = days;
    this.upcoming = upcoming;
  }
}
