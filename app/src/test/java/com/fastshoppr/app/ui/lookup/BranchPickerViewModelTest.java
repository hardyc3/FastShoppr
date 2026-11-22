/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.fastshoppr.app.data.remote.storelocator.BranchPageExtractor;
import com.fastshoppr.app.domain.id.IdGenerator;
import com.fastshoppr.app.domain.model.BranchPage;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorPages;
import com.fastshoppr.app.domain.usecase.ForgetSelectedStoreUseCase;
import com.fastshoppr.app.domain.usecase.GetSelectedStoresUseCase;
import com.fastshoppr.app.domain.usecase.SaveSelectedStoreUseCase;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests the WebView branch-picker state machine without a real WebView or network. */
public final class BranchPickerViewModelTest {

  @Rule
  public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  private SaveSelectedStoreUseCase saveSelectedStore;
  private ForgetSelectedStoreUseCase forgetSelectedStore;
  private GetSelectedStoresUseCase getSelectedStores;
  private BranchPageExtractor extractor;
  private BranchPickerViewModel viewModel;
  private RecordingEvents events;

  @Before
  public void setUp() {
    saveSelectedStore = mock(SaveSelectedStoreUseCase.class);
    forgetSelectedStore = mock(ForgetSelectedStoreUseCase.class);
    getSelectedStores = mock(GetSelectedStoresUseCase.class);
    extractor = new BranchPageExtractor();
    when(getSelectedStores.execute(any(StoreBrand.class))).thenReturn(null);
    events = new RecordingEvents();

    SavedStateHandle savedState = new SavedStateHandle();
    savedState.set(NavArguments.BRAND, StoreBrand.HOME_DEPOT.name());
    viewModel =
        new BranchPickerViewModel(
            extractor,
            saveSelectedStore,
            forgetSelectedStore,
            getSelectedStores,
            mock(IdGenerator.class),
            savedState);
    viewModel.getEvents().observeForever(events);
  }

  @Test
  public void initialStateUsesRequestedBrandAndItsLocatorUrl() {
    BranchPickerViewModel.PickerState state = viewModel.getState().getValue();

    assertEquals(StoreBrand.HOME_DEPOT, state.getBrand());
    assertEquals(StoreLocatorPages.locatorUrl(StoreBrand.HOME_DEPOT), state.getLocatorUrl());
    assertNull(state.getSelected());
  }

  @Test
  public void selectBrandIgnoresUnsupportedOrCurrentBrand() {
    viewModel.selectBrand(StoreBrand.CUSTOM);
    assertEquals(StoreBrand.HOME_DEPOT, viewModel.getState().getValue().getBrand());

    viewModel.selectBrand(StoreBrand.HOME_DEPOT);
    assertEquals(StoreBrand.HOME_DEPOT, viewModel.getState().getValue().getBrand());
    assertTrue(events.messages.isEmpty());
  }

  @Test
  public void selectBrandSwitchesLocatorPageAndClearsPendingBranch() {
    viewModel.previewCurrentPage(branchPageJson("Store One", "1 Main St", "Denver", "CO", "80202"));
    events.previews.clear();

    viewModel.selectBrand(StoreBrand.TARGET);
    assertTrue(events.previews.isEmpty());
    assertEquals(StoreBrand.TARGET, viewModel.getState().getValue().getBrand());
    assertEquals(
        StoreLocatorPages.locatorUrl(StoreBrand.TARGET),
        viewModel.getState().getValue().getLocatorUrl());

    viewModel.savePendingBranch();
    verify(saveSelectedStore, never()).execute(any(NearbyStore.class));
  }

  @Test
  public void previewCurrentPageOffersExtractedBranchForConfirmation() {
    viewModel.previewCurrentPage(branchPageJson("Store One", "1 Main St", "Denver", "CO", "80202"));

    assertEquals(1, events.previews.size());
    BranchPage page = events.previews.get(0);
    assertEquals("Store One", page.name());
    assertEquals("1 Main St", page.addressLine());
    assertEquals("80202", page.postalCode());
    assertEquals("Store One\n1 Main St\nDenver CO", BranchPickerViewModel.describe(page));
  }

  @Test
  public void previewCurrentPageRejectsChallengeAndEmptyPages() {
    viewModel.previewCurrentPage(
        pageJson(
            "Access Denied",
            "",
            "",
            "",
            "",
            "Access Denied",
            "Robot or human verification required"));
    assertEquals(BranchPickerViewModel.PickerMessage.CHALLENGE_PAGE, events.messages.get(0));

    viewModel.previewCurrentPage(pageJson("", "", "", "", "", "", ""));
    assertEquals(BranchPickerViewModel.PickerMessage.EXTRACTION_FAILED, events.messages.get(1));
    assertTrue(events.previews.isEmpty());
  }

  @Test
  public void savePendingBranchPersistsConfirmedBranchAndPublishesEvent() {
    NearbyStore saved = branch("Saved Branch", "42 Federal Blvd", "Denver", "CO");
    when(getSelectedStores.execute(StoreBrand.HOME_DEPOT)).thenReturn(saved);

    viewModel.previewCurrentPage(
        branchPageJson("Saved Branch", "42 Federal Blvd", "Denver", "CO", "80211"));
    viewModel.savePendingBranch();

    org.mockito.ArgumentCaptor<NearbyStore> captor =
        org.mockito.ArgumentCaptor.forClass(NearbyStore.class);
    verify(saveSelectedStore).execute(captor.capture());
    NearbyStore captured = captor.getValue();
    assertEquals(StoreBrand.HOME_DEPOT, captured.getBrand());
    assertEquals("Saved Branch", captured.getName());
    assertEquals("42 Federal Blvd", captured.getAddressLine());
    assertEquals("Denver", captured.getCity());
    assertEquals("CO", captured.getState());
    assertNull(captured.getPostalCode());
    assertNull(captured.getLatitude());
    assertNull(captured.getLongitude());
    assertEquals(NearbyStoreSource.LOCATOR, captured.getSource());
    assertEquals(StoreBrand.HOME_DEPOT, events.saveCompleted.get(0));
    assertEquals(saved, viewModel.getState().getValue().getSelected());
  }

  @Test
  public void saveWithoutPreviewShowsError() {
    viewModel.savePendingBranch();

    verify(saveSelectedStore, never()).execute(any(NearbyStore.class));
    assertEquals(BranchPickerViewModel.PickerMessage.EXTRACTION_FAILED, events.messages.get(0));
  }

  @Test
  public void forgetSelectedBranchClearsStateAndPublishesEvent() {
    when(getSelectedStores.execute(StoreBrand.HOME_DEPOT)).thenReturn(null);

    viewModel.forgetSelectedBranch();

    verify(forgetSelectedStore).execute(StoreBrand.HOME_DEPOT);
    assertNull(viewModel.getState().getValue().getSelected());
    assertEquals(StoreBrand.HOME_DEPOT, events.branchRemoved.get(0));
  }

  private static String branchPageJson(
      String name, String address, String city, String state, String postalCode) {
    return pageJson(name, address, city, state, postalCode, "Store details", "Open until 10pm");
  }

  private static String pageJson(
      String name,
      String address,
      String city,
      String state,
      String postalCode,
      String title,
      String bodyText) {
    String json =
        "{\"name\":\""
            + name
            + "\",\"addressLine\":\""
            + address
            + "\",\"city\":\""
            + city
            + "\",\"state\":\""
            + state
            + "\",\"postalCode\":\""
            + postalCode
            + "\",\"title\":\""
            + title
            + "\",\"bodyText\":\""
            + bodyText
            + "\"}";
    return new Gson().toJson(json);
  }

  private static NearbyStore branch(String name, String address, String city, String state) {
    return new NearbyStore(
        "home_depot-1",
        StoreBrand.HOME_DEPOT,
        name,
        address,
        city,
        state,
        null,
        null,
        null,
        NearbyStoreSource.LOCATOR);
  }

  private static final class RecordingEvents
      implements androidx.lifecycle.Observer<SingleEvent<BranchPickerViewModel.PickerEvent>> {

    private final List<BranchPage> previews = new java.util.ArrayList<>();
    private final List<BranchPickerViewModel.PickerMessage> messages = new java.util.ArrayList<>();
    private final List<StoreBrand> saveCompleted = new java.util.ArrayList<>();
    private final List<StoreBrand> branchRemoved = new java.util.ArrayList<>();

    @Override
    public void onChanged(SingleEvent<BranchPickerViewModel.PickerEvent> wrapper) {
      BranchPickerViewModel.PickerEvent event = wrapper.getContentIfNotHandled();
      if (event instanceof BranchPickerViewModel.PickerEvent.BranchPreview preview) {
        previews.add(preview.page());
      } else if (event instanceof BranchPickerViewModel.PickerEvent.SaveCompleted completed) {
        saveCompleted.add(completed.brand());
      } else if (event instanceof BranchPickerViewModel.PickerEvent.BranchRemoved removed) {
        branchRemoved.add(removed.brand());
      } else if (event instanceof BranchPickerViewModel.PickerEvent.ShowMessage message) {
        messages.add(message.message());
      }
    }
  }
}
