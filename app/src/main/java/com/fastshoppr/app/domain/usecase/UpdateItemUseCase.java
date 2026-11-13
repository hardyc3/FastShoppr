/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ItemLocation;
import com.fastshoppr.app.domain.model.ListItem;
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
          boolean storeChanged = !Objects.equals(existing.getStoreId(), storeId);

          ListItem.Builder updated =
              ListItem.builderFrom(existing)
                  .rawText(rawText)
                  .displayName(cleanedDisplayName)
                  .normalizedText(TextNormalizer.normalizeForLookup(cleanedDisplayName))
                  .quantity(quantity)
                  .storeId(storeId)
                  .updatedAt(clock.nowMillis());

          if (storeChanged) {
            // an aisle resolved for another store's layout is meaningless; reset lookup state
            updated
                .aisle(location.aisle())
                .bay(location.bay())
                .matchedProductName(null)
                .matchedProductUrl(null)
                .matchConfidence(null)
                .lookupStatus(location.lookupStatus())
                .lookupSource(location.lookupSource())
                .lookupError(null)
                .matchCandidatesJson(null);
          } else if (locationChanged) {
            // manual override: keep the location, drop lookup metadata and errors
            updated
                .aisle(location.aisle())
                .bay(location.bay())
                .matchedProductName(location.isPresent() ? existing.getMatchedProductName() : null)
                .matchedProductUrl(location.isPresent() ? existing.getMatchedProductUrl() : null)
                .matchConfidence(location.isPresent() ? existing.getMatchConfidence() : null)
                .lookupStatus(location.lookupStatus())
                .lookupSource(location.lookupSource())
                .lookupError(null)
                .matchCandidatesJson(null);
          }

          int sortPosition = existing.getSortPosition();
          if (storeChanged) {
            sortPosition = itemRepository.nextSortPosition(existing.getListId(), storeId);
          }
          updated.sortPosition(sortPosition);

          ListItem result = updated.build();
          itemRepository.update(result);
          return result;
        },
        executor);
  }
}
