/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class CalendarUiStateTest {

  @Test
  public void stateCopiesInputListsAndExposesNoMutableViews() {
    List<CalendarDay> days =
        new ArrayList<>(List.of(new CalendarDay(LocalDate.of(2026, 11, 24), true, true, null)));
    List<ShoppingListSummary> upcoming = new ArrayList<>(List.of(summary()));

    CalendarUiState state = new CalendarUiState(YearMonth.of(2026, 11), days, upcoming);

    days.clear();
    upcoming.clear();
    assertEquals(1, state.days.size());
    assertEquals(1, state.upcoming.size());
    assertThrows(UnsupportedOperationException.class, () -> state.days.clear());
    assertThrows(UnsupportedOperationException.class, () -> state.upcoming.add(summary()));
  }

  @Test
  public void allStateFieldsAreFinal() throws Exception {
    for (Field field : CalendarUiState.class.getDeclaredFields()) {
      if (field.isSynthetic()) continue;
      assertTrue(field.getName() + " must be final", Modifier.isFinal(field.getModifiers()));
    }
  }

  private static ShoppingListSummary summary() {
    ShoppingList list =
        new ShoppingList(
            "list-1",
            "Shopping list",
            LocalDate.of(2026, 11, 24),
            null,
            false,
            false,
            null,
            1L,
            1L);
    return new ShoppingListSummary(list, 2, 1);
  }
}
