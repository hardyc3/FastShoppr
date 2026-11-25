/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.usecase;

import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.port.WebViewStateClearer;
import com.fastshoppr.app.domain.repository.SelectedStoreRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Removes a saved branch and all retailer WebView state tied to it. */
public final class ForgetSelectedStoreUseCase {

  private final SelectedStoreRepository repository;
  private final WebViewStateClearer webViewStateClearer;

  @Inject
  public ForgetSelectedStoreUseCase(
      SelectedStoreRepository repository, WebViewStateClearer webViewStateClearer) {
    this.repository = repository;
    this.webViewStateClearer = webViewStateClearer;
  }

  /** Wipes WebView state first, then clears the saved branch only after the wipe succeeds. */
  public CompletableFuture<Void> execute(StoreBrand brand) {
    return webViewStateClearer.clearAll().thenRun(() -> repository.clear(brand));
  }
}
