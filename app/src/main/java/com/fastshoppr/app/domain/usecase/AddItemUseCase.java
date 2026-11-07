/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.ShoppingList;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.ShoppingListRepository;
import com.fastshoppr.app.domain.text.TextNormalizer;
import com.fastshoppr.app.domain.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class AddItemUseCase {

  private final ShoppingListRepository listRepository;
  private final ListItemRepository itemRepository;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public AddItemUseCase(
      ShoppingListRepository listRepository,
      ListItemRepository itemRepository,
      IdGenerator idGenerator,
      Clock clock,
      ExecutorService executor) {
    this.listRepository = listRepository;
    this.itemRepository = itemRepository;
    this.idGenerator = idGenerator;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<ListItem> add(
      String listId,
      String storeId,
      String rawText,
      String displayName,
      String quantity,
      ItemSource source) {
    return CompletableFuture.supplyAsync(
        () -> {
          ShoppingList list = listRepository.getById(listId);
          String cleanedDisplayName = TextNormalizer.collapseWhitespace(displayName);
          if (cleanedDisplayName == null || cleanedDisplayName.isEmpty()) {
            cleanedDisplayName = rawText;
          }
          long now = clock.nowMillis();
          ListItem item =
              ListItem.builder()
                  .id(idGenerator.newId())
                  .listId(listId)
                  .storeId(storeId)
                  .rawText(rawText)
                  .displayName(cleanedDisplayName)
                  .normalizedText(TextNormalizer.normalizeForLookup(cleanedDisplayName))
                  .quantity(quantity)
                  .targetDate(list.getShoppingDate())
                  .lookupStatus(LookupStatus.PENDING)
                  .source(source == null ? ItemSource.TYPED : source)
                  .sortPosition(itemRepository.nextSortPosition(listId, storeId))
                  .createdAt(now)
                  .updatedAt(now)
                  .build();
          itemRepository.insert(item);
          return item;
        },
        executor);
  }
}
