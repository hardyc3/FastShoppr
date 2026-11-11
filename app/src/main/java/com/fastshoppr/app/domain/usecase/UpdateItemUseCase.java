/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ItemLocation;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.text.TextNormalizer;
import com.fastshoppr.app.domain.time.Clock;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class UpdateItemUseCase {

  private final ListItemRepository itemRepository;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public UpdateItemUseCase(
      ListItemRepository itemRepository, Clock clock, ExecutorService executor) {
    this.itemRepository = itemRepository;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<ListItem> update(
      String itemId,
      String rawText,
      String displayName,
      String quantity,
      String storeId,
      String aisle,
      String bay) {
    return CompletableFuture.supplyAsync(
        () -> {
          ListItem existing = itemRepository.getById(itemId);
          String cleanedDisplayName = TextNormalizer.collapseWhitespace(displayName);
          if (cleanedDisplayName == null || cleanedDisplayName.isEmpty()) {
            cleanedDisplayName = rawText;
          }

          ItemLocation location = ItemLocation.of(aisle, bay);
          boolean locationChanged =
              !Objects.equals(existing.getAisle(), location.aisle())
                  || !Objects.equals(existing.getBay(), location.bay());

          LookupStatus lookupStatus = existing.getLookupStatus();
          LookupSource lookupSource = existing.getLookupSource();
          if (locationChanged) {
            lookupStatus = location.lookupStatus();
            lookupSource = location.lookupSource();
          }

          int sortPosition = existing.getSortPosition();
          if (!Objects.equals(existing.getStoreId(), storeId)) {
            sortPosition = itemRepository.nextSortPosition(existing.getListId(), storeId);
          }

          ListItem updated =
              ListItem.builderFrom(existing)
                  .rawText(rawText)
                  .displayName(cleanedDisplayName)
                  .normalizedText(TextNormalizer.normalizeForLookup(cleanedDisplayName))
                  .quantity(quantity)
                  .storeId(storeId)
                  .aisle(location.aisle())
                  .bay(location.bay())
                  .lookupStatus(lookupStatus)
                  .lookupSource(lookupSource)
                  .sortPosition(sortPosition)
                  .updatedAt(clock.nowMillis())
                  .build();
          itemRepository.update(updated);
          return updated;
        },
        executor);
  }
}
