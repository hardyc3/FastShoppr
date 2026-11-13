/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.AiCleanupSource;
import com.fastshoppr.app.domain.model.CleanItemRequest;
import com.fastshoppr.app.domain.model.CleanItemResult;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Chooses the on-device model when enabled and available, otherwise the deterministic rules
 * cleaner. The deterministic path is always available, so cleanup never blocks the user.
 */
@Singleton
public final class ItemNameCleanerCoordinator {

  private final ItemNameCleaner aiCleaner;
  private final RuleBasedItemNameCleaner rulesCleaner;
  private final SettingsRepository settingsRepository;

  @Inject
  public ItemNameCleanerCoordinator(
      ItemNameCleaner aiCleaner,
      RuleBasedItemNameCleaner rulesCleaner,
      SettingsRepository settingsRepository) {
    this.aiCleaner = aiCleaner;
    this.rulesCleaner = rulesCleaner;
    this.settingsRepository = settingsRepository;
  }

  public CompletableFuture<AiAvailability> availability() {
    return aiCleaner.availability();
  }

  public CompletableFuture<CleanupOutcome> clean(
      CleanItemRequest request, AiAvailability availability) {
    if (!settingsRepository.isAiCleanupEnabled()
        || !(availability instanceof AiAvailability.Available)) {
      return rules(request);
    }
    return aiCleaner
        .clean(request)
        .thenApply(
            result -> {
              if (result instanceof CleanItemResult.Unavailable) {
                return new CleanupOutcome(
                    rulesCleaner.cleanText(request.getRawText()), AiCleanupSource.RULES);
              }
              return new CleanupOutcome(result, AiCleanupSource.ON_DEVICE_AI);
            })
        .exceptionally(
            error ->
                new CleanupOutcome(
                    rulesCleaner.cleanText(request.getRawText()), AiCleanupSource.RULES));
  }

  public CompletableFuture<Void> downloadModel(Consumer<Long> progressBytes) {
    return aiCleaner.downloadModel(progressBytes);
  }

  public void close() {
    aiCleaner.close();
  }

  private CompletableFuture<CleanupOutcome> rules(CleanItemRequest request) {
    return CompletableFuture.completedFuture(
        new CleanupOutcome(rulesCleaner.cleanText(request.getRawText()), AiCleanupSource.RULES));
  }
}
