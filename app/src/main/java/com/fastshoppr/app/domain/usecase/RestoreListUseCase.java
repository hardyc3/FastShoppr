/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class RestoreListUseCase {

  private final ShoppingListRepository listRepository;
  private final ExecutorService executor;

  @Inject
  public RestoreListUseCase(ShoppingListRepository listRepository, ExecutorService executor) {
    this.listRepository = listRepository;
    this.executor = executor;
  }

  public CompletableFuture<Void> restore(String listId) {
    return CompletableFuture.runAsync(() -> listRepository.restore(listId), executor);
  }
}
