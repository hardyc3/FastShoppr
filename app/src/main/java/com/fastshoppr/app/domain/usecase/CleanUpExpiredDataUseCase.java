/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.repository.AisleCacheRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/** Runs the scheduled retention work: expired aisle cache and 30-day soft-delete cleanup. */
public final class CleanUpExpiredDataUseCase {

  private final PurgeDeletedUseCase purgeDeletedUseCase;
  private final AisleCacheRepository cacheRepository;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public CleanUpExpiredDataUseCase(
      PurgeDeletedUseCase purgeDeletedUseCase,
      AisleCacheRepository cacheRepository,
      Clock clock,
      ExecutorService executor) {
    this.purgeDeletedUseCase = purgeDeletedUseCase;
    this.cacheRepository = cacheRepository;
    this.clock = clock;
    this.executor = executor;
  }

  /** Returns the number of soft-deleted rows removed. */
  public CompletableFuture<Integer> run() {
    return CompletableFuture.supplyAsync(
        () -> {
          long now = clock.nowMillis();
          cacheRepository.deleteExpired(now);
          return purgeDeletedUseCase.purgeExpired().join();
        },
        executor);
  }
}
