/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/** Applies a user-confirmed lookup candidate to an item (lookup candidates never beat MANUAL). */
public final class ApplyLookupCandidateUseCase {

  private final AisleLookupRepository lookupRepository;
  private final ExecutorService executor;

  @Inject
  public ApplyLookupCandidateUseCase(
      AisleLookupRepository lookupRepository, ExecutorService executor) {
    this.lookupRepository = lookupRepository;
    this.executor = executor;
  }

  public CompletableFuture<ListItem> apply(String itemId, StoreLookupResult.Found candidate) {
    return CompletableFuture.supplyAsync(
        () -> lookupRepository.applyCandidate(itemId, candidate), executor);
  }

  /** Synchronous apply for callers already running on a background executor. */
  public ListItem applyNow(String itemId, StoreLookupResult.Found candidate) {
    return lookupRepository.applyCandidate(itemId, candidate);
  }
}
