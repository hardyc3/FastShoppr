/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.text.TextNormalizer;
import com.fastshoppr.app.domain.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class SetItemAisleUseCase {

  private final ListItemRepository itemRepository;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public SetItemAisleUseCase(
      ListItemRepository itemRepository, Clock clock, ExecutorService executor) {
    this.itemRepository = itemRepository;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<ListItem> setAisle(String itemId, String aisle, String bay) {
    return CompletableFuture.supplyAsync(
        () -> {
          ListItem existing = itemRepository.getById(itemId);
          String cleanedAisle = TextNormalizer.trimToNull(aisle);
          String cleanedBay = TextNormalizer.trimToNull(bay);
          boolean hasManualLocation = cleanedAisle != null || cleanedBay != null;
          ListItem updated =
              ListItem.builderFrom(existing)
                  .aisle(cleanedAisle)
                  .bay(cleanedBay)
                  .lookupStatus(hasManualLocation ? LookupStatus.FOUND : LookupStatus.PENDING)
                  .lookupSource(hasManualLocation ? LookupSource.MANUAL : null)
                  .updatedAt(clock.nowMillis())
                  .build();
          itemRepository.update(updated);
          return updated;
        },
        executor);
  }
}
