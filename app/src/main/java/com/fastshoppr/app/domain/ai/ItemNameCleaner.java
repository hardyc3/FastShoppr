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
import java.util.function.Consumer;

/** Produces optional, user-confirmed suggestions for a shopping-list item name. */
public interface ItemNameCleaner {

  CompletableFuture<AiAvailability> availability();

  CompletableFuture<CleanItemResult> clean(CleanItemRequest request);

  /** Downloads the on-device model after an explicit user action. */
  default CompletableFuture<Void> downloadModel(Consumer<Long> progressBytes) {
    CompletableFuture<Void> unsupported = new CompletableFuture<>();
    unsupported.completeExceptionally(
        new UnsupportedOperationException("This cleaner has no downloadable model"));
    return unsupported;
  }

  void close();
}
