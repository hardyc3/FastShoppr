/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import java.util.List;
import java.util.Objects;

/** One item-name cleanup request with a bounded amount of nearby-item context. */
public final class CleanItemRequest {

  public static final int MAX_TEXT_LENGTH = 200;
  public static final int MAX_CONTEXT_ITEMS = 5;

  private final String rawText;
  private final List<String> context;

  public CleanItemRequest(String rawText, List<String> context) {
    if (rawText == null || rawText.isBlank()) {
      throw new IllegalArgumentException("rawText must not be blank");
    }
    String trimmed = rawText.trim();
    if (trimmed.length() > MAX_TEXT_LENGTH) {
      throw new IllegalArgumentException("rawText must be 200 characters or fewer");
    }
    List<String> safeContext = context == null ? List.of() : List.copyOf(context);
    if (safeContext.size() > MAX_CONTEXT_ITEMS) {
      throw new IllegalArgumentException("context must contain at most five items");
    }
    for (String item : safeContext) {
      Objects.requireNonNull(item, "context items must not be null");
    }
    this.rawText = trimmed;
    this.context = safeContext;
  }

  public String getRawText() {
    return rawText;
  }

  public List<String> getContext() {
    return context;
  }
}
