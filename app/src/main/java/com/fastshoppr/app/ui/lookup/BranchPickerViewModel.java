/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.data.remote.storelocator.BranchPageExtractor;
import com.fastshoppr.app.domain.concurrent.MainThreadExecutor;
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
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.stream.Stream;
import javax.inject.Inject;

/** Owns WebView branch-picker state. The WebView itself stays in the Fragment. */
@HiltViewModel
public final class BranchPickerViewModel extends ViewModel {

  public enum PickerMessage {
    NO_BRANCH_ON_PAGE,
    CHALLENGE_PAGE,
    EXTRACTION_FAILED,
    UNSUPPORTED_BRAND,
    WEB_VIEW_CLEAR_FAILED
  }

  public sealed interface PickerEvent
      permits PickerEvent.BranchPreview,
          PickerEvent.SaveCompleted,
          PickerEvent.BranchRemoved,
          PickerEvent.ShowMessage {

    record BranchPreview(BranchPage page) implements PickerEvent {}

    record SaveCompleted(StoreBrand brand) implements PickerEvent {}

    record BranchRemoved(StoreBrand brand) implements PickerEvent {}

    record ShowMessage(PickerMessage message) implements PickerEvent {}
  }

  public static final class PickerState {
    private final StoreBrand brand;
    private final String locatorUrl;
    private final NearbyStore selected;

    PickerState(StoreBrand brand, String locatorUrl, NearbyStore selected) {
      this.brand = brand;
      this.locatorUrl = locatorUrl;
      this.selected = selected;
    }

    public StoreBrand getBrand() {
      return brand;
    }

    public String getLocatorUrl() {
      return locatorUrl;
    }

    public NearbyStore getSelected() {
      return selected;
    }
  }

  private final BranchPageExtractor extractor;
  private final SaveSelectedStoreUseCase saveSelectedStore;
  private final ForgetSelectedStoreUseCase forgetSelectedStore;
  private final GetSelectedStoresUseCase getSelectedStores;
  private final IdGenerator idGenerator;
  private final MainThreadExecutor mainThread;
  private final MutableLiveData<PickerState> state = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<PickerEvent>> events = new MutableLiveData<>();

  private StoreBrand brand;
  private String pendingPageJson;

  @Inject
  public BranchPickerViewModel(
      BranchPageExtractor extractor,
      SaveSelectedStoreUseCase saveSelectedStore,
      ForgetSelectedStoreUseCase forgetSelectedStore,
      GetSelectedStoresUseCase getSelectedStores,
      IdGenerator idGenerator,
      MainThreadExecutor mainThread,
      SavedStateHandle savedStateHandle) {
    this.extractor = extractor;
    this.saveSelectedStore = saveSelectedStore;
    this.forgetSelectedStore = forgetSelectedStore;
    this.getSelectedStores = getSelectedStores;
    this.idGenerator = idGenerator;
    this.mainThread = mainThread;
    String requested = savedStateHandle.get(NavArguments.BRAND);
    this.brand = parseBrand(requested);
    publish();
  }

  public LiveData<PickerState> getState() {
    return state;
  }

  public LiveData<SingleEvent<PickerEvent>> getEvents() {
    return events;
  }

  public void selectBrand(StoreBrand requested) {
    if (!StoreLocatorPages.supports(requested) || requested == brand) {
      return;
    }
    brand = requested;
    pendingPageJson = null;
    publish();
  }

  /** Parses the current retailer page and asks the Fragment to confirm the branch with the user. */
  public void previewCurrentPage(String jsValue) {
    if (!StoreLocatorPages.supports(brand)) {
      postEvent(new PickerEvent.ShowMessage(PickerMessage.UNSUPPORTED_BRAND));
      return;
    }
    pendingPageJson = null;
    final BranchPage page;
    try {
      page = extractor.parse(jsValue);
    } catch (RuntimeException invalid) {
      postEvent(new PickerEvent.ShowMessage(PickerMessage.EXTRACTION_FAILED));
      return;
    }
    if (StoreLocatorPages.isChallengePage(page.title(), page.bodyText())) {
      postEvent(new PickerEvent.ShowMessage(PickerMessage.CHALLENGE_PAGE));
      return;
    }
    if (page.name().isEmpty()) {
      postEvent(new PickerEvent.ShowMessage(PickerMessage.NO_BRANCH_ON_PAGE));
      return;
    }
    pendingPageJson = jsValue;
    postEvent(new PickerEvent.BranchPreview(page));
  }

  /** Saves the branch the user just confirmed. */
  public void savePendingBranch() {
    if (pendingPageJson == null) {
      postEvent(new PickerEvent.ShowMessage(PickerMessage.EXTRACTION_FAILED));
      return;
    }
    BranchPage page = extractor.parse(pendingPageJson);
    int fingerprint = (page.name() + "|" + page.addressLine()).hashCode();
    NearbyStore store =
        new NearbyStore(
            brand.name().toLowerCase(java.util.Locale.US)
                + "-"
                + Integer.toString(fingerprint & 0x7fffffff),
            brand,
            page.name(),
            page.addressLine(),
            page.city(),
            page.state(),
            null,
            null,
            null,
            NearbyStoreSource.LOCATOR);
    saveSelectedStore.execute(store);
    pendingPageJson = null;
    publish();
    postEvent(new PickerEvent.SaveCompleted(brand));
  }

  public void forgetSelectedBranch() {
    pendingPageJson = null;
    forgetSelectedStore
        .execute(brand)
        .whenComplete(
            (ignored, error) ->
                mainThread.execute(
                    () -> {
                      publish();
                      if (error != null) {
                        postEvent(new PickerEvent.ShowMessage(PickerMessage.WEB_VIEW_CLEAR_FAILED));
                        return;
                      }
                      postEvent(new PickerEvent.BranchRemoved(brand));
                    }));
  }

  public static String describe(BranchPage page) {
    if (page == null) {
      return "";
    }
    StringBuilder label = new StringBuilder(page.name());
    if (!page.addressLine().isEmpty()) {
      label.append('\n').append(page.addressLine());
    }
    String locality =
        Stream.of(page.city(), page.state())
            .filter(value -> !value.isEmpty())
            .collect(java.util.stream.Collectors.joining(" "));
    if (!locality.isEmpty()) {
      label.append('\n').append(locality);
    }
    return label.toString();
  }

  private static StoreBrand parseBrand(String value) {
    if (value != null) {
      try {
        StoreBrand parsed = StoreBrand.valueOf(value);
        if (StoreLocatorPages.supports(parsed)) {
          return parsed;
        }
      } catch (IllegalArgumentException unknown) {
        // fall through to the default brand
      }
    }
    return StoreBrand.WALMART;
  }

  private void publish() {
    state.setValue(
        new PickerState(
            brand, StoreLocatorPages.locatorUrl(brand), getSelectedStores.execute(brand)));
  }

  private void postEvent(PickerEvent event) {
    events.setValue(new SingleEvent<>(event));
  }
}
