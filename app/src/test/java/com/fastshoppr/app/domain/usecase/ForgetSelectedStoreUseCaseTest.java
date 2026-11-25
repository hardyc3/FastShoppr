/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.port.WebViewStateClearer;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;

/** Verifies that forgetting a branch wipes WebView state before removing saved branch state. */
public final class ForgetSelectedStoreUseCaseTest {

  @Test
  public void clearsWebViewStateThenSavedBranch() {
    List<String> calls = new ArrayList<>();
    RecordingSelectedStoreRepository repository = new RecordingSelectedStoreRepository(calls);
    RecordingWebViewStateClearer clearer =
        new RecordingWebViewStateClearer(calls, CompletableFuture.completedFuture(null));
    ForgetSelectedStoreUseCase useCase = new ForgetSelectedStoreUseCase(repository, clearer);

    CompletableFuture<Void> cleared = useCase.execute(StoreBrand.WALMART);

    assertTrue(cleared.isDone());
    assertEquals(List.of(StoreBrand.WALMART), repository.clearedBrands);
    assertEquals(1, clearer.clearAllCalls);
    assertEquals(List.of("clearer.clearAll", "repository.clear"), calls);
  }

  @Test
  public void failedWebViewWipeKeepsSavedBranch() {
    List<String> calls = new ArrayList<>();
    RecordingSelectedStoreRepository repository = new RecordingSelectedStoreRepository(calls);
    CompletableFuture<Void> failed = new CompletableFuture<>();
    failed.completeExceptionally(new IllegalStateException("wipe failed"));
    RecordingWebViewStateClearer clearer = new RecordingWebViewStateClearer(calls, failed);
    ForgetSelectedStoreUseCase useCase = new ForgetSelectedStoreUseCase(repository, clearer);

    CompletableFuture<Void> cleared = useCase.execute(StoreBrand.WALMART);

    assertTrue(cleared.isCompletedExceptionally());
    assertTrue(repository.clearedBrands.isEmpty());
    assertEquals(List.of("clearer.clearAll"), calls);
    assertFalse(calls.contains("repository.clear"));
  }

  private static final class RecordingSelectedStoreRepository implements SelectedStoreRepository {

    private final List<String> calls;
    private final List<StoreBrand> clearedBrands = new ArrayList<>();

    RecordingSelectedStoreRepository(List<String> calls) {
      this.calls = calls;
    }

    @Override
    public NearbyStore getSelected(StoreBrand brand) {
      return null;
    }

    @Override
    public Map<StoreBrand, NearbyStore> getSelected() {
      return Map.of();
    }

    @Override
    public void select(NearbyStore store) {}

    @Override
    public void clear(StoreBrand brand) {
      clearedBrands.add(brand);
      calls.add("repository.clear");
    }
  }

  private static final class RecordingWebViewStateClearer implements WebViewStateClearer {

    private final List<String> calls;
    private final CompletableFuture<Void> result;
    private int clearAllCalls;

    RecordingWebViewStateClearer(List<String> calls, CompletableFuture<Void> result) {
      this.calls = calls;
      this.result = result;
    }

    @Override
    public CompletableFuture<Void> clearAll() {
      clearAllCalls++;
      calls.add("clearer.clearAll");
      return result;
    }
  }
}
