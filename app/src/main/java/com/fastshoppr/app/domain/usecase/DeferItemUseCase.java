/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.text.TextNormalizer;
import com.fastshoppr.app.domain.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class DeferItemUseCase {

  private final ListItemRepository itemRepository;
  private final ShoppingListRepository listRepository;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public DeferItemUseCase(
      ListItemRepository itemRepository,
      ShoppingListRepository listRepository,
      Clock clock,
      ExecutorService executor) {
    this.itemRepository = itemRepository;
    this.listRepository = listRepository;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<ListItem> defer(
      String itemId,
      LocalDate targetDate,
      String targetListName,
      String confirmedRawTextOrNull) {
    Objects.requireNonNull(targetDate, "targetDate");
    return CompletableFuture.supplyAsync(
        () -> {
          ShoppingList targetList =
              listRepository.getOrCreate(targetDate, targetListName, null, false);
          ListItem moved = itemRepository.moveToDate(itemId, targetDate, targetList.getName());
          if (confirmedRawTextOrNull != null) {
            String newRaw = TextNormalizer.collapseWhitespace(confirmedRawTextOrNull);
            if (newRaw != null && !newRaw.isBlank()) {
              ListItem updated =
                  ListItem.from(moved)
                      .rawText(newRaw)
                      .displayName(newRaw)
                      .normalizedText(TextNormalizer.normalizeForLookup(newRaw))
                      .updatedAt(clock.nowMillis())
                      .build();
              itemRepository.update(updated);
              return updated;
            }
          }
          return moved;
        },
        executor);
  }
}
