/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.CleanItemRequest;
import com.fastshoppr.app.domain.model.CleanItemResult;
import java.util.concurrent.CompletableFuture;

/** Cleaner that never suggests changes, used when the feature is disabled in tests. */
public final class NoOpItemNameCleaner implements ItemNameCleaner {

  @Override
  public CompletableFuture<AiAvailability> availability() {
    return CompletableFuture.completedFuture(new AiAvailability.Unavailable());
  }

  @Override
  public CompletableFuture<CleanItemResult> clean(CleanItemRequest request) {
    return CompletableFuture.completedFuture(new CleanItemResult.Unavailable());
  }

  @Override
  public void close() {
    // Nothing to close.
  }
}
