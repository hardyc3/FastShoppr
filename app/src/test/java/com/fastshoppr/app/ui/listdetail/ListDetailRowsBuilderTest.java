/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.usecase.SortItemsUseCase;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

public final class ListDetailRowsBuilderTest {

  private final ListDetailRowsBuilder builder = new ListDetailRowsBuilder(new SortItemsUseCase());

  @Test
  public void groupsByStoreAndPlacesUnresolvedItemsAfterAttentionHeader() {
    Store walmart = store("walmart", StoreBrand.WALMART, "Walmart");
    Store target = store("target", StoreBrand.TARGET, "Target");

    List<ListDetailRow> rows =
        builder.build(
            List.of(walmart, target),
            List.of(item("a", "walmart", "10"), item("b", "walmart", null)),
            SortDirection.ASC,
            storeId -> "draft-" + storeId,
            7L);

    assertEquals(new ListDetailRow.StoreHeader(walmart, 2), rows.get(0));
    assertItemId(rows.get(1), "a");
    assertEquals(new ListDetailRow.AttentionHeader("walmart"), rows.get(2));
    assertItemId(rows.get(3), "b");
    assertEquals(new ListDetailRow.AddItem(walmart, "draft-walmart", 7L), rows.get(4));
    assertEquals(new ListDetailRow.StoreHeader(target, 0), rows.get(5));
    assertEquals(new ListDetailRow.AddItem(target, "draft-target", 7L), rows.get(6));
  }

  @Test
  public void descendingSortKeepsUnresolvedItemsLast() {
    Store walmart = store("walmart", StoreBrand.WALMART, "Walmart");

    List<ListDetailRow> rows =
        builder.build(
            List.of(walmart),
            List.of(
                item("a", "walmart", "2"), item("b", "walmart", "10"), item("c", "walmart", "?")),
            SortDirection.DESC,
            storeId -> "",
            0L);

    assertItemId(rows.get(1), "b");
    assertItemId(rows.get(2), "a");
    assertEquals(new ListDetailRow.AttentionHeader("walmart"), rows.get(3));
    assertItemId(rows.get(4), "c");
  }

  private static void assertItemId(ListDetailRow row, String expectedId) {
    assertTrue(row instanceof ListDetailRow.Item);
    assertEquals(expectedId, ((ListDetailRow.Item) row).item().getId());
  }

  private static Store store(String id, StoreBrand brand, String name) {
    return new Store(
        id, brand, name, null, null, null, null, null, null, false, false, false, null, 1, 1);
  }

  private static ListItem item(String id, String storeId, String aisle) {
    return ListItem.builder()
        .id(id)
        .listId("list-1")
        .storeId(storeId)
        .rawText(id)
        .targetDate(LocalDate.of(2026, 11, 8))
        .aisle(aisle)
        .createdAt(1)
        .updatedAt(1)
        .build();
  }
}
