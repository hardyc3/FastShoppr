/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.sort;

import static org.junit.Assert.assertEquals;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.SortDirection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class ListItemComparatorTest {

  @Test
  public void unresolvedAislesSortLastInAscendingAndDescending() {
    List<ListItem> items =
        new ArrayList<>(
            List.of(item("a", "10", null, 0), item("b", null, null, 1), item("c", "2", null, 2)));

    items.sort(new ListItemComparator(SortDirection.ASC));
    assertEquals(List.of("c", "a", "b"), ids(items));

    items.sort(new ListItemComparator(SortDirection.DESC));
    assertEquals(List.of("a", "c", "b"), ids(items));
  }

  @Test
  public void tiesBreakByBayThenSortPosition() {
    List<ListItem> items =
        new ArrayList<>(
            List.of(
                item("a", "5", "10", 0),
                item("b", "5", "2", 1),
                item("c", "5", null, 2),
                item("d", "5", null, 3)));

    items.sort(new ListItemComparator(SortDirection.ASC));

    assertEquals(List.of("b", "a", "c", "d"), ids(items));
  }

  @Test
  public void unresolvedBaysSortLastInAscendingAndDescending() {
    List<ListItem> items =
        new ArrayList<>(
            List.of(
                item("a", "5", "10", 0),
                item("b", "5", "2", 1),
                item("c", "5", null, 2),
                item("d", "5", null, 3)));

    items.sort(new ListItemComparator(SortDirection.ASC));
    assertEquals(List.of("b", "a", "c", "d"), ids(items));

    items.sort(new ListItemComparator(SortDirection.DESC));
    assertEquals(List.of("a", "b", "d", "c"), ids(items));
  }

  private static List<String> ids(List<ListItem> items) {
    return items.stream().map(ListItem::getId).toList();
  }

  private static ListItem item(String id, String aisle, String bay, int sortPosition) {
    return ListItem.builder()
        .id(id)
        .listId("list")
        .storeId("store")
        .rawText("milk")
        .targetDate(LocalDate.of(2026, 11, 7))
        .aisle(aisle)
        .bay(bay)
        .sortPosition(sortPosition)
        .createdAt(1)
        .updatedAt(1)
        .build();
  }
}
