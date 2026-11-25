/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.port;

import java.util.concurrent.CompletableFuture;

/** Clears retailer WebView state such as cookies and DOM storage. */
public interface WebViewStateClearer {

  /** Completes only after cookie and DOM removal has finished. */
  CompletableFuture<Void> clearAll();
}
