/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

public final class CalendarAdaptersTest {

  @Test
  public void dayAdapterUsesListAdapterSubmitListAndDiffUtil() throws Exception {
    CalendarDayAdapter adapter = new CalendarDayAdapter(day -> {});

    assertTrue(adapter instanceof ListAdapter);
    assertNotNull(ListAdapter.class.getMethod("submitList", List.class));
    assertSameCallback(CalendarDayAdapter.DIFF_CALLBACK, adapter);
  }

  @Test
  public void dayDiffCallbackComparesIdentityAndContent() {
    LocalDate day = LocalDate.of(2026, 11, 24);
    CalendarDay original = new CalendarDay(day, true, false, null);
    CalendarDay sameDay = new CalendarDay(day, true, true, null);
    CalendarDay paddingA = new CalendarDay(null, false, false, null);
    CalendarDay paddingB = new CalendarDay(null, false, false, null);

    assertTrue(CalendarDayAdapter.DIFF_CALLBACK.areItemsTheSame(original, sameDay));
    assertFalse(CalendarDayAdapter.DIFF_CALLBACK.areContentsTheSame(original, sameDay));
    assertFalse(CalendarDayAdapter.DIFF_CALLBACK.areItemsTheSame(paddingA, paddingB));
  }

  @Test
  public void upcomingAdapterUsesListAdapterSubmitListAndDiffUtil() throws Exception {
    CalendarUpcomingAdapter adapter = new CalendarUpcomingAdapter(summary -> {});

    assertTrue(adapter instanceof ListAdapter);
    assertNotNull(ListAdapter.class.getMethod("submitList", List.class));
    assertSameCallback(CalendarUpcomingAdapter.DIFF_CALLBACK, adapter);
  }

  @Test
  public void upcomingDiffCallbackComparesByListIdAndContent() {
    ShoppingListSummary first = summary("list-1", 1);
    ShoppingListSummary changedCount = summary("list-1", 9);
    ShoppingListSummary otherList = summary("list-2", 1);

    assertTrue(CalendarUpcomingAdapter.DIFF_CALLBACK.areItemsTheSame(first, changedCount));
    assertFalse(CalendarUpcomingAdapter.DIFF_CALLBACK.areContentsTheSame(first, changedCount));
    assertFalse(CalendarUpcomingAdapter.DIFF_CALLBACK.areItemsTheSame(first, otherList));
  }

  /**
   * Confirms the adapter was constructed with the exposed DiffUtil callback, so Fragment calls to
   * {@code submitList} are diffed rather than manually re-rendered.
   */
  private static void assertSameCallback(
      DiffUtil.ItemCallback<?> expected, ListAdapter<?, ?> adapter) throws Exception {
    java.lang.reflect.Field differField = ListAdapter.class.getDeclaredField("mDiffer");
    differField.setAccessible(true);
    Object differ = differField.get(adapter);
    java.lang.reflect.Field configField = differ.getClass().getDeclaredField("mConfig");
    configField.setAccessible(true);
    Object config = configField.get(differ);
    java.lang.reflect.Method getCallback = config.getClass().getMethod("getDiffCallback");
    assertEquals(expected, getCallback.invoke(config));
  }

  private static ShoppingListSummary summary(String id, int itemCount) {
    ShoppingList list =
        new ShoppingList(
            id, "Shopping list", LocalDate.of(2026, 11, 24), null, false, false, null, 1L, 1L);
    return new ShoppingListSummary(list, itemCount, 1);
  }
}
