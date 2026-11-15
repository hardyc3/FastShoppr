/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.SortDirection;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

public final class SortItemsUseCaseTest {

  @Test
  public void sortsAscendingAndDescendingWithUnknownLast() {
    List<ListItem> items =
        List.of(item("a", "10"), item("b", null), item("c", "2"), item("d", "EC2"));
    SortItemsUseCase useCase = new SortItemsUseCase();

    assertEquals(List.of("d", "c", "a", "b"), ids(useCase.sort(items, SortDirection.ASC)));
    assertEquals(List.of("a", "c", "d", "b"), ids(useCase.sort(items, SortDirection.DESC)));
  }

  private static List<String> ids(List<ListItem> items) {
    return items.stream().map(ListItem::getId).toList();
  }

  private static ListItem item(String id, String aisle) {
    return ListItem.builder()
        .id(id)
        .listId("list")
        .storeId("store")
        .rawText("milk")
        .targetDate(LocalDate.of(2026, 11, 7))
        .aisle(aisle)
        .sortPosition(0)
        .createdAt(1)
        .updatedAt(1)
        .build();
  }
}
