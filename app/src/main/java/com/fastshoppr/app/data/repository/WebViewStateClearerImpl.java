/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.repository;

import android.webkit.CookieManager;
import android.webkit.WebStorage;
import com.fastshoppr.app.domain.concurrent.MainThreadExecutor;
import com.fastshoppr.app.domain.port.WebViewStateClearer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Android implementation of the shared retailer WebView wipe (D-043/D-049). */
@Singleton
public final class WebViewStateClearerImpl implements WebViewStateClearer {

  private final MainThreadExecutor mainThreadExecutor;

  @Inject
  public WebViewStateClearerImpl(MainThreadExecutor mainThreadExecutor) {
    this.mainThreadExecutor = mainThreadExecutor;
  }

  @Override
  public CompletableFuture<Void> clearAll() {
    CompletableFuture<Void> cleared = new CompletableFuture<>();
    mainThreadExecutor.execute(
        () -> clearWebViewDataOnMain(() -> cleared.complete(null), cleared::completeExceptionally));
    return cleared;
  }

  private static void clearWebViewDataOnMain(Runnable onComplete, Consumer<Throwable> onError) {
    try {
      WebStorage.getInstance().deleteAllData();
      CookieManager.getInstance().removeAllCookies(ignored -> onComplete.run());
    } catch (RuntimeException error) {
      onError.accept(error);
    }
  }
}
