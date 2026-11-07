/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class PurgeDeletedUseCase {

  public static final Duration RETENTION = Duration.ofDays(30);

  private final ShoppingListRepository listRepository;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public PurgeDeletedUseCase(
      ShoppingListRepository listRepository, Clock clock, ExecutorService executor) {
    this.listRepository = listRepository;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<Integer> purgeExpired() {
    return CompletableFuture.supplyAsync(
        () -> listRepository.purgeDeletedBefore(clock.nowMillis() - RETENTION.toMillis()),
        executor);
  }
}
