/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.CleanItemRequest;
import java.util.List;
import org.junit.Test;

public final class CleanItemPromptBuilderTest {

  @Test
  public void promptContainsItemAndNearbyContext() {
    CleanItemRequest request = new CleanItemRequest("tp", List.of("paper towels", "soap"));

    String prompt = CleanItemPromptBuilder.buildUserPrompt(request);

    assertTrue(prompt.contains("Item: tp"));
    assertTrue(prompt.contains("Nearby items: paper towels; soap"));
    assertTrue(prompt.contains("displayName"));
    assertTrue(prompt.contains("confidence"));
    assertTrue(prompt.contains("reason"));
  }

  @Test
  public void promptOmitsContextSectionWhenNoNearbyItems() {
    String prompt = CleanItemPromptBuilder.buildUserPrompt(new CleanItemRequest("milk", List.of()));

    assertTrue(prompt.contains("Item: milk"));
    assertFalse(prompt.contains("Nearby items"));
  }

  @Test
  public void systemInstructionRequiresJsonOnly() {
    assertTrue(CleanItemPromptBuilder.SYSTEM_INSTRUCTION.contains("Return JSON only"));
    assertTrue(CleanItemPromptBuilder.SYSTEM_INSTRUCTION.contains("Do not invent brands"));
  }
}
