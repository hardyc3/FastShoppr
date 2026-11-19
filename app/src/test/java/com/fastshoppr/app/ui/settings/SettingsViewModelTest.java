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
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorBlockedException;
import com.fastshoppr.app.domain.repository.LocationSource;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.usecase.DeleteAllDataUseCase;
import com.fastshoppr.app.domain.usecase.FindNearbyStoresUseCase;
import com.fastshoppr.app.domain.usecase.ForgetSelectedStoreUseCase;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import com.fastshoppr.app.domain.usecase.SaveSelectedStoreUseCase;
import java.util.Collections;
import java.util.List;
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
  private final LocationSource locationSource = mock(LocationSource.class);
  private final FindNearbyStoresUseCase findNearbyStores = mock(FindNearbyStoresUseCase.class);
  private final SaveSelectedStoreUseCase saveSelectedStore = mock(SaveSelectedStoreUseCase.class);
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
        Runnable::run,
        locationSource,
        findNearbyStores,
        saveSelectedStore,
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

  @Test
  public void savePostalCodePersistsAndPublishes() {
    viewModel.savePostalCode("  90210  ");

    verify(saveSelectedStore).savePostalCode("90210");
    assertEquals("90210", viewModel.getState().getValue().getPostalCode());
    assertEquals(
        SettingsViewModel.StoreMessage.ZIP_SAVED,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void blankPostalCodeReportsErrorWithoutSaving() {
    viewModel.savePostalCode("   ");

    verify(saveSelectedStore, never()).savePostalCode(any());
    assertEquals(
        SettingsViewModel.StoreMessage.ZIP_REQUIRED,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void manualStoreIsSavedForSelectedBrand() {
    viewModel.saveManualStore(StoreBrand.MACYS, "  Macy's Union Square ", "123 Market St");

    org.mockito.ArgumentCaptor<NearbyStore> captor =
        org.mockito.ArgumentCaptor.forClass(NearbyStore.class);
    verify(saveSelectedStore).execute(captor.capture());
    NearbyStore saved = captor.getValue();
    assertEquals(StoreBrand.MACYS, saved.getBrand());
    assertEquals("Macy's Union Square", saved.getName());
    assertEquals("123 Market St", saved.getAddressLine());
    assertEquals(NearbyStoreSource.MANUAL, saved.getSource());
  }

  @Test
  public void savedStoreAppearsInStateAfterSelection() {
    NearbyStore store =
        new NearbyStore(
            "store-1",
            StoreBrand.WALMART,
            "Walmart Supercenter",
            "1 Main St",
            null,
            null,
            "90210",
            null,
            null,
            NearbyStoreSource.MANUAL);
    when(getSelectedStores.execute()).thenReturn(Map.of(StoreBrand.WALMART, store));

    viewModel.saveStore(store);

    assertEquals(
        store, viewModel.getState().getValue().getSelectedStores().get(StoreBrand.WALMART));
    assertEquals(
        SettingsViewModel.StoreMessage.STORE_SAVED,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void postalCodeSearchRequiresSavedZip() {
    viewModel.findNearbyUsingPostalCode(StoreBrand.WALMART);

    verify(findNearbyStores, never()).execute(any(), any(), any());
    assertEquals(
        SettingsViewModel.StoreMessage.ZIP_REQUIRED,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void postalCodeSearchPublishesCandidatesWithSearchedBrand() {
    when(getSelectedStores.getPostalCode()).thenReturn("90210");
    viewModel = newViewModel();
    NearbyStore store = nearbyStore();
    when(findNearbyStores.execute(StoreBrand.TARGET, null, "90210"))
        .thenReturn(CompletableFuture.completedFuture(List.of(store)));

    viewModel.findNearbyUsingPostalCode(StoreBrand.TARGET);

    SettingsViewModel.StoreSearchResults results =
        viewModel.getNearbyStoresEvent().getValue().getContentIfNotHandled();
    assertEquals(StoreBrand.TARGET, results.getBrand());
    assertEquals(List.of(store), results.getStores());
    assertFalse(viewModel.getState().getValue().isStoreSearching());
  }

  @Test
  public void postalCodeSearchReportsNoResultsMessage() {
    when(getSelectedStores.getPostalCode()).thenReturn("90210");
    viewModel = newViewModel();
    when(findNearbyStores.execute(StoreBrand.WALMART, null, "90210"))
        .thenReturn(CompletableFuture.completedFuture(List.of()));

    viewModel.findNearbyUsingPostalCode(StoreBrand.WALMART);

    assertEquals(
        SettingsViewModel.StoreMessage.NO_RESULTS,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void blockedSearchReportsBlockedMessage() {
    when(getSelectedStores.getPostalCode()).thenReturn("90210");
    viewModel = newViewModel();
    when(findNearbyStores.execute(StoreBrand.WALMART, null, "90210"))
        .thenReturn(
            failedFuture(
                new StoreLocatorBlockedException("The store locator blocked this request")));

    viewModel.findNearbyUsingPostalCode(StoreBrand.WALMART);

    assertEquals(
        SettingsViewModel.StoreMessage.BLOCKED,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void failedSearchReportsGenericMessage() {
    when(getSelectedStores.getPostalCode()).thenReturn("90210");
    viewModel = newViewModel();
    when(findNearbyStores.execute(StoreBrand.WALMART, null, "90210"))
        .thenReturn(failedFuture(new IllegalStateException("network down")));

    viewModel.findNearbyUsingPostalCode(StoreBrand.WALMART);

    assertEquals(
        SettingsViewModel.StoreMessage.SEARCH_FAILED,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
  }

  @Test
  public void locationSearchReportsUnavailableWhenNoFixExists() {
    when(locationSource.lastKnownLocation()).thenReturn(CompletableFuture.completedFuture(null));

    viewModel.findNearbyUsingLocation(StoreBrand.HOME_DEPOT);

    assertEquals(
        SettingsViewModel.StoreMessage.LOCATION_UNAVAILABLE,
        viewModel.getStoreMessageEvent().getValue().getContentIfNotHandled());
    verify(findNearbyStores, never()).execute(any(), any(), any());
  }

  private void assertMapped(AiAvailability availability, SettingsViewModel.AiStatus expected) {
    when(cleanerCoordinator.availability())
        .thenReturn(CompletableFuture.completedFuture(availability));

    viewModel.load();

    assertEquals(expected, viewModel.getState().getValue().getAiStatus());
  }

  private static NearbyStore nearbyStore() {
    return new NearbyStore(
        "store-1",
        StoreBrand.TARGET,
        "Target #1",
        "1 Main St",
        "Springfield",
        "IL",
        "62701",
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
