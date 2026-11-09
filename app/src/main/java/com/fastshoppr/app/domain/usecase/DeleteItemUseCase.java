/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.repository.ListItemRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class DeleteItemUseCase {

  private final ListItemRepository itemRepository;
  private final ExecutorService executor;

  @Inject
  public DeleteItemUseCase(ListItemRepository itemRepository, ExecutorService executor) {
    this.itemRepository = itemRepository;
    this.executor = executor;
  }

  public CompletableFuture<Void> delete(String itemId) {
    return CompletableFuture.runAsync(() -> itemRepository.softDelete(itemId), executor);
  }
}
