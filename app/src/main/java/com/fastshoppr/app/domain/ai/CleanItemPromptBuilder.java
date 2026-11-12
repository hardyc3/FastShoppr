/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import com.fastshoppr.app.domain.model.CleanItemRequest;
import java.util.List;

/** Builds the constrained prompt used for on-device item-name cleanup. */
public final class CleanItemPromptBuilder {

  public static final String SYSTEM_INSTRUCTION =
      "You clean shopping-list item names. Preserve meaning. Fix obvious spelling errors, "
          + "expand common abbreviations, and remove list markers. Do not invent brands, sizes, "
          + "or products. Return JSON only with fields: displayName, confidence, reason. "
          + "If no change is needed, return displayName equal to the input.";

  private CleanItemPromptBuilder() {}

  public static String buildUserPrompt(CleanItemRequest request) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Item: ").append(request.getRawText()).append('\n');
    List<String> context = request.getContext();
    if (!context.isEmpty()) {
      prompt.append("Nearby items: ");
      for (int index = 0; index < context.size(); index++) {
        if (index > 0) {
          prompt.append("; ");
        }
        prompt.append(context.get(index));
      }
      prompt.append('\n');
    }
    prompt.append(
        "Return JSON only: {\"displayName\":\"...\",\"confidence\":0.0,\"reason\":\"...\"}");
    return prompt.toString();
  }
}
