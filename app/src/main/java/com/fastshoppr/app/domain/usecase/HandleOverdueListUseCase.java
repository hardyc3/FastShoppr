/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.date.LocalDateTimes;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class HandleOverdueListUseCase {

  public enum Action {
    MOVE_UNCHECKED,
    KEEP,
    MISSED
  }

  private final ShoppingListRepository listRepository;
  private final ListItemRepository itemRepository;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public HandleOverdueListUseCase(
      ShoppingListRepository listRepository,
      ListItemRepository itemRepository,
      Clock clock,
      ExecutorService executor) {
    this.listRepository = listRepository;
    this.itemRepository = itemRepository;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<Void> apply(
      Action action, String pastListId, String targetListIdOrNull, LocalDate createDateOrNull) {
    return CompletableFuture.runAsync(
        () -> {
          if (action == Action.KEEP) return;
          if (action == Action.MISSED) {
            // archive list (missed representation) - items stay in archived list, not carried
            // forward
            listRepository.archive(pastListId);
            return;
          }
          if (action == Action.MOVE_UNCHECKED) {
            ShoppingList target;
            if (targetListIdOrNull != null) {
              target = listRepository.getById(targetListIdOrNull);
            } else if (createDateOrNull != null) {
              target =
                  listRepository.getOrCreate(
                      createDateOrNull, "Shopping list " + createDateOrNull, null, false);
            } else {
              throw new IllegalArgumentException("target required");
            }
            itemRepository.moveUncheckedItemsToList(
                pastListId, target.getId(), target.getShoppingDate());
          }
        },
        executor);
  }

  public LocalDate today() {
    return LocalDateTimes.today(clock);
  }
}
