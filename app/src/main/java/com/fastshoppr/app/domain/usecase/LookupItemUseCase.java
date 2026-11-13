/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class LookupItemUseCase {

  private final AisleLookupRepository lookupRepository;
  private final ExecutorService executor;

  @Inject
  public LookupItemUseCase(AisleLookupRepository lookupRepository, ExecutorService executor) {
    this.lookupRepository = lookupRepository;
    this.executor = executor;
  }

  public CompletableFuture<AisleLookupRepository.LookupOutcome> lookup(
      String itemId, boolean forceRefresh) {
    return CompletableFuture.supplyAsync(
        () -> lookupRepository.lookupItem(itemId, forceRefresh), executor);
  }

  /** Synchronous lookup for callers already running on a background executor. */
  public AisleLookupRepository.LookupOutcome lookupNow(String itemId, boolean forceRefresh) {
    return lookupRepository.lookupItem(itemId, forceRefresh);
  }
}
