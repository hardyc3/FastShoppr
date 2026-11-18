/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.fastshoppr.app.domain.ai.ItemNameCleanerCoordinator;
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.usecase.DeleteAllDataUseCase;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public final class SettingsViewModelTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  private final SettingsRepository settingsRepository = mock(SettingsRepository.class);
  private final ItemNameCleanerCoordinator cleanerCoordinator =
      mock(ItemNameCleanerCoordinator.class);
  private final DeleteAllDataUseCase deleteAllDataUseCase = mock(DeleteAllDataUseCase.class);

  private SettingsViewModel viewModel;

  @Before
  public void setUp() {
    when(deleteAllDataUseCase.execute()).thenReturn(CompletableFuture.completedFuture(null));
    viewModel =
        new SettingsViewModel(
            settingsRepository, cleanerCoordinator, deleteAllDataUseCase, Runnable::run);
  }

  @Test
  public void readsPersistedToggleState() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);

    viewModel =
        new SettingsViewModel(
            settingsRepository, cleanerCoordinator, deleteAllDataUseCase, Runnable::run);
    SettingsViewModel.SettingsUiState state = viewModel.getState().getValue();

    assertTrue(state.isAiCleanupEnabled());
    assertEquals(SettingsViewModel.AiStatus.CHECKING, state.getAiStatus());
  }

  @Test
  public void setAiCleanupEnabledPersistsAndPublishes() {
    viewModel.setAiCleanupEnabled(true);

    verify(settingsRepository).setAiCleanupEnabled(true);
    assertTrue(viewModel.getState().getValue().isAiCleanupEnabled());

    viewModel.setAiCleanupEnabled(false);

    verify(settingsRepository).setAiCleanupEnabled(false);
    assertFalse(viewModel.getState().getValue().isAiCleanupEnabled());
  }

  @Test
  public void loadMapsEveryAvailabilityState() {
    assertMapped(new AiAvailability.Available(), SettingsViewModel.AiStatus.AVAILABLE);
    assertMapped(new AiAvailability.Downloadable(), SettingsViewModel.AiStatus.DOWNLOADABLE);
    assertMapped(new AiAvailability.Downloading(), SettingsViewModel.AiStatus.DOWNLOADING);
    assertMapped(new AiAvailability.QuotaExceeded(), SettingsViewModel.AiStatus.QUOTA_EXCEEDED);
    assertMapped(new AiAvailability.Unavailable(), SettingsViewModel.AiStatus.UNAVAILABLE);

    when(cleanerCoordinator.availability())
        .thenReturn(failedFuture(new IllegalStateException("boom")));
    viewModel.load();
    assertEquals(
        SettingsViewModel.AiStatus.UNAVAILABLE, viewModel.getState().getValue().getAiStatus());
  }

  @Test
  public void downloadReportsProgressAndRefreshesStatus() {
    when(cleanerCoordinator.availability())
        .thenReturn(CompletableFuture.completedFuture(new AiAvailability.Downloadable()));
    CompletableFuture<Void> download = new CompletableFuture<>();
    when(cleanerCoordinator.downloadModel(any()))
        .thenAnswer(
            invocation -> {
              Consumer<Long> progress = invocation.getArgument(0);
              progress.accept(2_048L);
              return download;
            });
    viewModel.load();

    viewModel.downloadModel();

    SettingsViewModel.SettingsUiState downloading = viewModel.getState().getValue();
    assertEquals(SettingsViewModel.AiStatus.DOWNLOADING, downloading.getAiStatus());
    assertEquals(2_048L, downloading.getDownloadedBytes());

    download.complete(null);

    assertEquals(
        SettingsViewModel.AiStatus.DOWNLOADABLE, viewModel.getState().getValue().getAiStatus());
  }

  @Test
  public void downloadFailureIsReported() {
    when(cleanerCoordinator.availability())
        .thenReturn(CompletableFuture.completedFuture(new AiAvailability.Downloadable()));
    CompletableFuture<Void> download = new CompletableFuture<>();
    when(cleanerCoordinator.downloadModel(any())).thenReturn(download);
    viewModel.load();

    viewModel.downloadModel();
    download.completeExceptionally(new IllegalStateException("no network"));

    assertEquals(
        SettingsViewModel.AiStatus.DOWNLOAD_FAILED, viewModel.getState().getValue().getAiStatus());
  }

  @Test
  public void deleteAllDataPublishesOneSuccessEvent() {
    viewModel.deleteAllData();

    verify(deleteAllDataUseCase).execute();
    assertTrue(viewModel.getDeleteEvent().getValue().getContentIfNotHandled());
    assertNull(viewModel.getDeleteEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void deleteAllDataPublishesFailureEvent() {
    when(deleteAllDataUseCase.execute())
        .thenReturn(failedFuture(new IllegalStateException("disk failure")));

    viewModel.deleteAllData();

    assertFalse(viewModel.getDeleteEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void downloadIsIgnoredUnlessModelIsDownloadable() {
    when(cleanerCoordinator.availability())
        .thenReturn(CompletableFuture.completedFuture(new AiAvailability.Available()));
    viewModel.load();

    viewModel.downloadModel();

    verify(cleanerCoordinator, never()).downloadModel(any());
  }

  private void assertMapped(AiAvailability availability, SettingsViewModel.AiStatus expected) {
    when(cleanerCoordinator.availability())
        .thenReturn(CompletableFuture.completedFuture(availability));

    viewModel.load();

    assertEquals(expected, viewModel.getState().getValue().getAiStatus());
  }

  private static <T> CompletableFuture<T> failedFuture(Throwable error) {
    CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(error);
    return future;
  }
}
