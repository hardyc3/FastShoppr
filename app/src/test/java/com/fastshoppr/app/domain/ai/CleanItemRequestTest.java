/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.CleanItemRequest;
import java.util.List;
import org.junit.Test;

public final class CleanItemRequestTest {

  @Test
  public void trimsTextAndCopiesContext() {
    CleanItemRequest request = new CleanItemRequest("  milk  ", List.of("eggs"));

    assertEquals("milk", request.getRawText());
    assertEquals(List.of("eggs"), request.getContext());
  }

  @Test
  public void treatsNullContextAsEmpty() {
    CleanItemRequest request = new CleanItemRequest("milk", null);

    assertTrue(request.getContext().isEmpty());
  }

  @Test
  public void rejectsBlankText() {
    assertThrows(IllegalArgumentException.class, () -> new CleanItemRequest("   ", List.of()));
    assertThrows(IllegalArgumentException.class, () -> new CleanItemRequest(null, List.of()));
  }

  @Test
  public void acceptsTwoHundredCharactersAndRejectsMore() {
    String allowed = "x".repeat(CleanItemRequest.MAX_TEXT_LENGTH);
    assertEquals(allowed, new CleanItemRequest(allowed, List.of()).getRawText());

    assertThrows(
        IllegalArgumentException.class, () -> new CleanItemRequest(allowed + "x", List.of()));
  }

  @Test
  public void capsContextAtFiveItems() {
    List<String> allowed = List.of("a", "b", "c", "d", "e");
    assertEquals(allowed, new CleanItemRequest("milk", allowed).getContext());

    assertThrows(
        IllegalArgumentException.class,
        () -> new CleanItemRequest("milk", List.of("a", "b", "c", "d", "e", "f")));
  }
}
