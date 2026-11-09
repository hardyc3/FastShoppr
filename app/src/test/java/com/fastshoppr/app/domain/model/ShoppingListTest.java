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

public final class ShoppingListTest {

  @Test
  public void trimsAndCollapsesName() {
    ShoppingList list = list("  Weekly   shop ");

    assertEquals("Weekly shop", list.getName());
  }

  @Test
  public void rejectsBlankAndOversizedName() {
    assertThrows(IllegalArgumentException.class, () -> list("  "));
    assertThrows(IllegalArgumentException.class, () -> list("x".repeat(81)));
  }

  @Test
  public void rejectsMissingDate() {
    assertThrows(
        NullPointerException.class,
        () -> new ShoppingList("id", "List", null, null, false, false, null, 1, 1));
  }

  private static ShoppingList list(String name) {
    return new ShoppingList("id", name, LocalDate.of(2026, 11, 7), null, false, false, null, 1, 1);
  }
}
