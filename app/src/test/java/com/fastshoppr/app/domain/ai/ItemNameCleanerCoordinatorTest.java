/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.AiCleanupSource;
import com.fastshoppr.app.domain.model.CleanItemRequest;
import com.fastshoppr.app.domain.model.CleanItemResult;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;

public final class ItemNameCleanerCoordinatorTest {

  private final ItemNameCleaner aiCleaner = mock(ItemNameCleaner.class);
  private final SettingsRepository settingsRepository = mock(SettingsRepository.class);
  private final CleanItemRequest request = new CleanItemRequest("tp", List.of());

  private ItemNameCleanerCoordinator coordinator;

  @Before
  public void setUp() {
    coordinator =
        new ItemNameCleanerCoordinator(
            aiCleaner, new RuleBasedItemNameCleaner(), settingsRepository);
  }

  @Test
  public void usesRulesWhenFeatureIsDisabled() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(false);

    CleanupOutcome outcome = coordinator.clean(request, new AiAvailability.Available()).join();

    assertEquals(AiCleanupSource.RULES, outcome.source());
    assertEquals("Toilet paper", suggestedName(outcome));
    verify(aiCleaner, never()).clean(any());
  }

  @Test
  public void usesAiWhenEnabledAndAvailable() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    when(aiCleaner.clean(request))
        .thenReturn(
            CompletableFuture.completedFuture(
                new CleanItemResult.Suggested("Toilet tissue", 0.9f, "Model output")));

    CleanupOutcome outcome = coordinator.clean(request, new AiAvailability.Available()).join();

    assertEquals(AiCleanupSource.ON_DEVICE_AI, outcome.source());
    assertEquals("Toilet tissue", suggestedName(outcome));
  }

  @Test
  public void fallsBackToRulesWhenModelIsUnavailable() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);

    CleanupOutcome outcome = coordinator.clean(request, new AiAvailability.Downloadable()).join();

    assertEquals(AiCleanupSource.RULES, outcome.source());
    assertEquals("Toilet paper", suggestedName(outcome));
    verify(aiCleaner, never()).clean(any());
  }

  @Test
  public void fallsBackToRulesWhenAiReturnsUnavailable() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    when(aiCleaner.clean(request))
        .thenReturn(CompletableFuture.completedFuture(new CleanItemResult.Unavailable()));

    CleanupOutcome outcome = coordinator.clean(request, new AiAvailability.Available()).join();

    assertEquals(AiCleanupSource.RULES, outcome.source());
    assertEquals("Toilet paper", suggestedName(outcome));
  }

  @Test
  public void fallsBackToRulesWhenAiFails() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    when(aiCleaner.clean(request)).thenReturn(failedFuture(new IllegalStateException("timeout")));

    CleanupOutcome outcome = coordinator.clean(request, new AiAvailability.Available()).join();

    assertEquals(AiCleanupSource.RULES, outcome.source());
    assertEquals("Toilet paper", suggestedName(outcome));
  }

  @Test
  public void reportsAiNoChangeWithoutRulesOverride() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);
    when(aiCleaner.clean(request))
        .thenReturn(CompletableFuture.completedFuture(new CleanItemResult.NoChange()));

    CleanupOutcome outcome = coordinator.clean(request, new AiAvailability.Available()).join();

    assertEquals(AiCleanupSource.ON_DEVICE_AI, outcome.source());
    assertType(CleanItemResult.NoChange.class, outcome.result());
  }

  @Test
  public void delegatesAvailabilityDownloadAndClose() {
    CompletableFuture<AiAvailability> availability =
        CompletableFuture.completedFuture(new AiAvailability.Available());
    when(aiCleaner.availability()).thenReturn(availability);

    assertEquals(availability.join(), coordinator.availability().join());

    coordinator.downloadModel(bytes -> {});
    verify(aiCleaner).downloadModel(any());

    coordinator.close();
    verify(aiCleaner).close();
  }

  private static String suggestedName(CleanupOutcome outcome) {
    return ((CleanItemResult.Suggested) outcome.result()).displayName();
  }

  private static <T> T assertType(Class<T> type, Object value) {
    assertTrue("expected " + type.getSimpleName(), type.isInstance(value));
    return type.cast(value);
  }

  private static <T> CompletableFuture<T> failedFuture(Throwable error) {
    CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(error);
    return future;
  }
}
