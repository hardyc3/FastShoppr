/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

/** Result of asking an item-name cleaner for a user-facing name. */
public sealed interface CleanItemResult
    permits CleanItemResult.Suggested, CleanItemResult.NoChange, CleanItemResult.Unavailable {

  record Suggested(String displayName, float confidence, String reason)
      implements CleanItemResult {}

  record NoChange() implements CleanItemResult {}

  record Unavailable() implements CleanItemResult {}
}
