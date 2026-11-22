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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.fastshoppr.app.domain.ai.ItemNameCleanerCoordinator;
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.usecase.DeleteAllDataUseCase;
import com.fastshoppr.app.domain.usecase.ForgetSelectedStoreUseCase;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import java.util.Collections;
import java.util.Map;
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
  private final ForgetSelectedStoreUseCase forgetSelectedStore =
      mock(ForgetSelectedStoreUseCase.class);
  private final GetSelectedStoresUseCase getSelectedStores = mock(GetSelectedStoresUseCase.class);

  private SettingsViewModel viewModel;

  @Before
  public void setUp() {
    when(deleteAllDataUseCase.execute()).thenReturn(CompletableFuture.completedFuture(null));
    when(getSelectedStores.execute()).thenReturn(Collections.emptyMap());
    viewModel = newViewModel();
  }

  private SettingsViewModel newViewModel() {
    return new SettingsViewModel(
        settingsRepository,
        cleanerCoordinator,
        deleteAllDataUseCase,
        forgetSelectedStore,
        getSelectedStores);
  }

  @Test
  public void readsPersistedToggleState() {
    when(settingsRepository.isAiCleanupEnabled()).thenReturn(true);

    viewModel = newViewModel();
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
  public void downloadIsIgnoredUnlessModelIsDownloadable() {
    when(cleanerCoordinator.availability())
        .thenReturn(CompletableFuture.completedFuture(new AiAvailability.Available()));
    viewModel.load();

    viewModel.downloadModel();

    verify(cleanerCoordinator, never()).downloadModel(any());
  }

  @Test
  public void savedBranchesAppearInStatePerBrand() {
    Map<StoreBrand, NearbyStore> saved = Map.of(StoreBrand.WALMART, store(StoreBrand.WALMART));
    when(getSelectedStores.execute()).thenReturn(saved);

    viewModel = newViewModel();

    assertEquals(saved, viewModel.getState().getValue().getSelectedStores());
    assertEquals(1, viewModel.getState().getValue().getSelectedStores().size());
  }

  @Test
  public void refreshReloadsBranchesAfterReturningFromBranchPicker() {
    when(getSelectedStores.execute())
        .thenReturn(Collections.emptyMap(), Map.of(StoreBrand.TARGET, store(StoreBrand.TARGET)));
    viewModel = newViewModel();
    assertTrue(viewModel.getState().getValue().getSelectedStores().isEmpty());

    viewModel.refresh();

    assertTrue(viewModel.getState().getValue().getSelectedStores().containsKey(StoreBrand.TARGET));
  }

  @Test
  public void forgetStoreClearsItsBranchAndEmitsMessage() {
    NearbyStore walmart = store(StoreBrand.WALMART);
    when(getSelectedStores.execute())
        .thenReturn(Map.of(StoreBrand.WALMART, walmart), Collections.emptyMap());
    viewModel = newViewModel();
    assertEquals(
        walmart, viewModel.getState().getValue().getSelectedStores().get(StoreBrand.WALMART));

    viewModel.forgetStore(StoreBrand.WALMART);

    verify(forgetSelectedStore).execute(StoreBrand.WALMART);
    assertTrue(viewModel.getState().getValue().getSelectedStores().isEmpty());
    assertEquals(
        SettingsViewModel.StoreMessage.STORE_REMOVED,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
    assertNull(viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void deleteAllDataPublishesOneSuccessEventAndRefreshesBranches() {
    viewModel.deleteAllData();

    verify(deleteAllDataUseCase).execute();
    assertTrue(viewModel.getDeleteEvent().getValue().getContentIfNotHandled());
    assertNull(viewModel.getDeleteEvent().getValue().getContentIfNotHandled());
    verify(getSelectedStores, times(2)).execute();
  }

  @Test
  public void deleteAllDataPublishesFailureEvent() {
    when(deleteAllDataUseCase.execute())
        .thenReturn(failedFuture(new IllegalStateException("disk failure")));

    viewModel.deleteAllData();

    assertFalse(viewModel.getDeleteEvent().getValue().getContentIfNotHandled());
  }

  private void assertMapped(AiAvailability availability, SettingsViewModel.AiStatus expected) {
    when(cleanerCoordinator.availability())
        .thenReturn(CompletableFuture.completedFuture(availability));

    viewModel.load();

    assertEquals(expected, viewModel.getState().getValue().getAiStatus());
  }

  private static NearbyStore store(StoreBrand brand) {
    return new NearbyStore(
        brand.name(),
        brand,
        brand.getDisplayName() + " #123",
        "1 Main St",
        null,
        null,
        null,
        null,
        null,
        NearbyStoreSource.LOCATOR);
  }

  private static <T> CompletableFuture<T> failedFuture(Throwable error) {
    CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(error);
    return future;
  }
}
