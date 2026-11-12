/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.ReviewedItem;
import com.fastshoppr.app.domain.repository.CaptureRepository;
import com.fastshoppr.app.domain.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/** Turns user-approved review lines into items and completes the capture atomically. */
public final class SaveReviewedItemsUseCase {

  private final CaptureRepository captureRepository;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final ExecutorService executor;

  @Inject
  public SaveReviewedItemsUseCase(
      CaptureRepository captureRepository,
      IdGenerator idGenerator,
      Clock clock,
      ExecutorService executor) {
    this.captureRepository = captureRepository;
    this.idGenerator = idGenerator;
    this.clock = clock;
    this.executor = executor;
  }

  public CompletableFuture<Void> save(
      String captureId, String listId, ItemSource source, List<ReviewedItem> reviewedItems) {
    return CompletableFuture.runAsync(
        () -> {
          long now = clock.nowMillis();
          List<ListItem> items = new ArrayList<>(reviewedItems.size());
          for (ReviewedItem reviewed : reviewedItems) {
            items.add(
                ListItem.builder()
                    .id(idGenerator.newId())
                    .listId(listId)
                    .storeId(reviewed.storeId())
                    .rawText(reviewed.text())
                    .displayName(reviewed.displayName())
                    .targetDate(reviewed.targetDate())
                    .source(source)
                    .isChecked(false)
                    .sortPosition(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
          }
          captureRepository.saveReviewed(captureId, items, now);
        },
        executor);
  }
}
