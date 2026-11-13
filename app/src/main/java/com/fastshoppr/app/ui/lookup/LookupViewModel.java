/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.repository.AisleLookupRepository;
import com.fastshoppr.app.domain.repository.ListItemRepository;
import com.fastshoppr.app.domain.repository.SettingsRepository;
import com.fastshoppr.app.domain.repository.StoreRepository;
import com.fastshoppr.app.domain.time.Clock;
import com.fastshoppr.app.domain.usecase.ApplyLookupCandidateUseCase;
import com.fastshoppr.app.domain.usecase.LookupItemUseCase;
import com.fastshoppr.app.domain.usecase.SetItemAisleUseCase;
import com.fastshoppr.app.ui.navigation.NavArguments;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@HiltViewModel
public final class LookupViewModel extends ViewModel {

  public static final class Progress {
    public final int done;
    public final int total;
    public final String status;
    public final String logLine;
    public final boolean running;
    public final boolean stopped;
    public final StoreLookupResult.Multiple multiple;
    public final String multipleItemId;

    public Progress(
        int done,
        int total,
        String status,
        String logLine,
        boolean running,
        boolean stopped,
        StoreLookupResult.Multiple multiple,
        String multipleItemId) {
      this.done = done;
      this.total = total;
      this.status = status;
      this.logLine = logLine;
      this.running = running;
      this.stopped = stopped;
      this.multiple = multiple;
      this.multipleItemId = multipleItemId;
    }
  }

  private final String listId;
  private final ListItemRepository itemRepository;
  private final StoreRepository storeRepository;
  private final SettingsRepository settingsRepository;
  private final LookupItemUseCase lookupItemUseCase;
  private final SetItemAisleUseCase setItemAisleUseCase;
  private final ApplyLookupCandidateUseCase applyLookupCandidateUseCase;
  private final ExecutorService executor;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private final MutableLiveData<String> storeContext = new MutableLiveData<>("");
  private final MutableLiveData<Boolean> hasWalmart = new MutableLiveData<>(false);
  private final MutableLiveData<Progress> progress = new MutableLiveData<>();
  private final StringBuilder log = new StringBuilder();
  private volatile boolean running;

  private final Clock clock;

  @Inject
  public LookupViewModel(
      ListItemRepository itemRepository,
      StoreRepository storeRepository,
      SettingsRepository settingsRepository,
      LookupItemUseCase lookupItemUseCase,
      SetItemAisleUseCase setItemAisleUseCase,
      ApplyLookupCandidateUseCase applyLookupCandidateUseCase,
      ExecutorService executor,
      Clock clock,
      SavedStateHandle savedStateHandle) {
    this.itemRepository = itemRepository;
    this.storeRepository = storeRepository;
    this.settingsRepository = settingsRepository;
    this.lookupItemUseCase = lookupItemUseCase;
    this.setItemAisleUseCase = setItemAisleUseCase;
    this.applyLookupCandidateUseCase = applyLookupCandidateUseCase;
    this.executor = executor;
    this.clock = clock;
    this.listId = Objects.requireNonNull(savedStateHandle.get(NavArguments.LIST_ID));
  }

  public LiveData<String> getStoreContext() {
    return storeContext;
  }

  /** True when the list contains at least one Walmart item; gates the store-picker button. */
  public LiveData<Boolean> getHasWalmart() {
    return hasWalmart;
  }

  public LiveData<Progress> getProgress() {
    return progress;
  }

  public String getListId() {
    return listId;
  }

  public void loadContext() {
    executor.execute(
        () -> {
          List<ListItem> items = itemRepository.getByList(listId);
          String context = "";
          boolean walmartPresent = false;
          for (ListItem item : items) {
            Store store = resolveStoreForItem(item);
            if (store != null) {
              if (store.getBrand() == StoreBrand.WALMART) {
                walmartPresent = true;
              }
              if (context.isEmpty()) {
                String addr = store.getAddressLine();
                context = store.getName() + (addr == null || addr.isBlank() ? "" : " — " + addr);
              }
            }
          }
          boolean finalWalmart = walmartPresent;
          String finalContext = context;
          mainHandler.post(
              () -> {
                storeContext.setValue(finalContext);
                hasWalmart.setValue(finalWalmart);
              });
        });
  }

  public void start(boolean forceRefresh) {
    if (running) {
      return;
    }
    running = true;
    log.setLength(0);
    postProgress(0, 0, "Starting lookup…", "Starting lookup…", true, false, null, null);
    executor.execute(() -> run(forceRefresh));
  }

  private void run(boolean forceRefresh) {
    List<ListItem> items = itemRepository.getByList(listId);
    List<ListItem> eligible = new ArrayList<>();
    for (ListItem item : items) {
      if (item.getLookupSource() == LookupSource.MANUAL && !forceRefresh) {
        continue;
      }
      eligible.add(item);
    }
    int total = eligible.size();
    int found = 0;
    int needsReview = 0;
    int notFound = 0;
    int errors = 0;

    for (int i = 0; i < eligible.size(); i++) {
      ListItem item = eligible.get(i);
      String label = item.getDisplayName();
      postProgress(i, total, "Checking " + label, null, true, false, null, null);
      try {
        AisleLookupRepository.LookupOutcome outcome =
            lookupItemUseCase.lookupNow(item.getId(), forceRefresh);
        String line = formatOutcome(outcome, item);
        append(line);
        if (outcome.blocked) {
          running = false;
          postProgress(i + 1, total, "Lookup stopped: " + line, line, false, true, null, null);
          return;
        }
        if (outcome.item != null) {
          switch (outcome.item.getLookupStatus()) {
            case FOUND -> found++;
            case NEEDS_REVIEW -> needsReview++;
            case NOT_FOUND -> notFound++;
            case ERROR -> errors++;
            default -> {}
          }
        }
        if (outcome.result instanceof StoreLookupResult.Multiple multiple) {
          postProgress(
              i, total, "Choose a match for " + label, line, true, false, multiple, item.getId());
          // wait briefly is not possible here; UI will handle dialog and user can continue by
          // tapping start again
          // but to keep harness simple we pause run by stopping after prompting
          running = false;
          postProgress(
              i + 1,
              total,
              "Selection needed for " + label,
              line,
              false,
              false,
              multiple,
              item.getId());
          return;
        }
      } catch (Exception e) {
        errors++;
        append("Error: " + label + " - " + e.getMessage());
      }
      postProgress(i + 1, total, "Checked " + label, null, true, false, null, null);
    }
    running = false;
    String summary =
        "Found "
            + found
            + ", needs review "
            + needsReview
            + ", not found "
            + notFound
            + ", errors "
            + errors
            + ".";
    append(summary);
    postProgress(total, total, summary, summary, false, false, null, null);
  }

  public void applyCandidate(String itemId, StoreLookupResult.Found candidate) {
    executor.execute(
        () -> {
          try {
            ListItem updated = applyLookupCandidateUseCase.applyNow(itemId, candidate);
            append(
                updated.getLookupSource() == LookupSource.MANUAL
                    ? "Manual value kept for " + updated.getDisplayName()
                    : "Applied candidate: "
                        + (candidate.getProductName() == null
                            ? updated.getDisplayName()
                            : candidate.getProductName()));
          } catch (Exception e) {
            append("Failed to apply candidate: " + e.getMessage());
          }
        });
  }

  private Store resolveStoreForItem(ListItem item) {
    try {
      if (item.getStoreId() != null) {
        return storeRepository.getById(item.getStoreId());
      }
    } catch (Exception ignored) {
    }
    try {
      String last = settingsRepository.getLastUsedStoreId();
      if (last != null) return storeRepository.getById(last);
    } catch (Exception ignored) {
    }
    try {
      String def = settingsRepository.getDefaultStoreId();
      if (def != null) return storeRepository.getById(def);
    } catch (Exception ignored) {
    }
    try {
      List<Store> active = storeRepository.getActive();
      if (active != null && !active.isEmpty()) return active.get(0);
    } catch (Exception ignored) {
    }
    return null;
  }

  private String formatOutcome(AisleLookupRepository.LookupOutcome outcome, ListItem original) {
    if (outcome == null || outcome.item == null) return "No result";
    ListItem item = outcome.item;
    String name = item.getDisplayName();
    switch (item.getLookupStatus()) {
      case FOUND:
        return "Found: "
            + name
            + " → "
            + (item.getAisle() == null ? "aisle" : item.getAisle())
            + (outcome.stale ? " (saved, may be outdated)" : "");
      case NEEDS_REVIEW:
        return "Needs review: " + name;
      case NOT_FOUND:
        return "Not found: " + name;
      case ERROR:
        return "Error: "
            + name
            + (item.getLookupError() == null ? "" : " - " + item.getLookupError());
      default:
        return "Checked: " + name;
    }
  }

  private void append(String line) {
    if (line == null) return;
    synchronized (log) {
      if (log.length() > 0) log.append('\n');
      log.append(line);
    }
  }

  public String getLogText() {
    synchronized (log) {
      return log.toString();
    }
  }

  private void postProgress(
      int done,
      int total,
      String status,
      String logLine,
      boolean runningNow,
      boolean stopped,
      StoreLookupResult.Multiple multiple,
      String multipleItemId) {
    if (logLine != null) append(logLine);
    mainHandler.post(
        () ->
            progress.setValue(
                new Progress(
                    done, total, status, null, runningNow, stopped, multiple, multipleItemId)));
  }
}
