/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class MoveItemToDateUseCase {

  private final ListItemRepository itemRepository;
  private final ExecutorService executor;

  @Inject
  public MoveItemToDateUseCase(ListItemRepository itemRepository, ExecutorService executor) {
    this.itemRepository = itemRepository;
    this.executor = executor;
  }

  public CompletableFuture<ListItem> move(
      String itemId, LocalDate targetDate, String targetListName) {
    return CompletableFuture.supplyAsync(
        () -> itemRepository.moveToDate(itemId, targetDate, targetListName), executor);
  }
}
