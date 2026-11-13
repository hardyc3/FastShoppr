/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.testing;

import com.fastshoppr.app.domain.ai.ItemNameCleaner;
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.CleanItemRequest;
import com.fastshoppr.app.domain.model.CleanItemResult;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Deterministic cleaner used by instrumentation tests so no live model is called. */
public final class FakeItemNameCleaner implements ItemNameCleaner {

  private static final Map<String, String> SUGGESTIONS =
      Map.of("tp", "Toilet paper", "pb", "Peanut butter", "gal", "Gallon");

  @Override
  public CompletableFuture<AiAvailability> availability() {
    return CompletableFuture.completedFuture(new AiAvailability.Available());
  }

  @Override
  public CompletableFuture<CleanItemResult> clean(CleanItemRequest request) {
    String suggestion = SUGGESTIONS.get(request.getRawText());
    if (suggestion == null) {
      return CompletableFuture.completedFuture(new CleanItemResult.NoChange());
    }
    return CompletableFuture.completedFuture(
        new CleanItemResult.Suggested(suggestion, 0.9f, "Test suggestion"));
  }

  @Override
  public CompletableFuture<Void> downloadModel(Consumer<Long> progressBytes) {
    if (progressBytes != null) {
      progressBytes.accept(1_024L);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void close() {
    // Stateless.
  }
}
