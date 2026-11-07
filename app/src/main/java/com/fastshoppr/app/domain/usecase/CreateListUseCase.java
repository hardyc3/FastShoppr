/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class CreateListUseCase {

  private final ShoppingListRepository listRepository;
  private final ExecutorService executor;

  @Inject
  public CreateListUseCase(ShoppingListRepository listRepository, ExecutorService executor) {
    this.listRepository = listRepository;
    this.executor = executor;
  }

  public CompletableFuture<ShoppingList> create(
      LocalDate shoppingDate, String name, String scheduleId, boolean isAutoCreated) {
    return CompletableFuture.supplyAsync(
        () -> listRepository.getOrCreate(shoppingDate, name, scheduleId, isAutoCreated), executor);
  }
}
