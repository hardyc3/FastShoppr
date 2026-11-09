/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.LocalDate;
import org.junit.Test;

public final class ListItemTest {

  @Test
  public void rejectsBlankRawText() {
    assertThrows(IllegalArgumentException.class, () -> builder().rawText("   ").build());
  }

  @Test
  public void rejectsOversizedRawText() {
    assertThrows(IllegalArgumentException.class, () -> builder().rawText("x".repeat(201)).build());
  }

  @Test
  public void defaultsDisplayNameToRawTextAndCollapsesWhitespace() {
    ListItem defaultValue = builder().rawText("  milk ").displayName(null).build();
    ListItem cleanedValue = builder().rawText("milk").displayName("  Whole   Milk ").build();

    assertEquals("milk", defaultValue.getDisplayName());
    assertEquals("Whole Milk", cleanedValue.getDisplayName());
  }

  @Test
  public void derivesNormalizedTextWhenAbsent() {
    ListItem item = builder().rawText("Paper Towels!").displayName("Paper Towels!").build();

    assertEquals("paper towels", item.getNormalizedText());
  }

  @Test
  public void rejectsInvalidConfidence() {
    assertThrows(IllegalArgumentException.class, () -> builder().matchConfidence(1.5f).build());
  }

  private static ListItem.Builder builder() {
    return ListItem.builder()
        .id("item")
        .listId("list")
        .storeId("store")
        .rawText("milk")
        .targetDate(LocalDate.of(2026, 11, 7))
        .createdAt(1)
        .updatedAt(1);
  }
}
