/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.walmart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.fastshoppr.app.domain.error.AppError;
import com.fastshoppr.app.domain.lookup.StoreLookupRequest;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.lookup.StoreLookupService;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.google.gson.JsonParser;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Walmart lookup via WebView extraction (D-004/D-022).
 *
 * <p>Must be used as a user-initiated foreground session. This minimal implementation runs on the
 * main thread when possible and is intended for foreground harness use; full foreground-service
 * orchestration belongs to Phase 7.
 *
 * <p>The physical store context shown in the UI is the retailer's default store; it cannot be
 * pinned programmatically until store records carry a retailer store number. Results from an
 * unverified context are downgraded below the auto-select threshold so they are always flagged for
 * review rather than shown as a confident aisle.
 */
@Singleton
public class WalmartWebViewLookupService implements StoreLookupService {

  private static final long TIMEOUT_MS = 20_000L;
  private static final long EXTRACT_DELAY_MS = 1_500L;

  private final Context appContext;
  private final ExecutorService executor;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  @Inject
  public WalmartWebViewLookupService(
      @ApplicationContext Context appContext, ExecutorService executor) {
    this.appContext = appContext.getApplicationContext();
    this.executor = executor;
  }

  @Override
  public StoreBrand getBrand() {
    return StoreBrand.WALMART;
  }

  @Override
  public CompletableFuture<StoreLookupResult> lookup(StoreLookupRequest request) {
    CompletableFuture<StoreLookupResult> future = new CompletableFuture<>();
    Runnable task = () -> startLookup(request, future);
    if (Looper.myLooper() == Looper.getMainLooper()) {
      task.run();
    } else {
      mainHandler.post(task);
    }
    return future;
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void startLookup(
      StoreLookupRequest request, CompletableFuture<StoreLookupResult> future) {
    if (future.isDone()) {
      return;
    }
    final WebView webView = new WebView(appContext);
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setAllowFileAccessFromFileURLs(false);
    settings.setAllowUniversalAccessFromFileURLs(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setSupportMultipleWindows(false);
    settings.setUserAgentString(settings.getUserAgentString() + " FastShoppr/1.0");
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptCookie(true);
    cookieManager.setAcceptThirdPartyCookies(webView, false);

    final AtomicBoolean tornDown = new AtomicBoolean(false);
    final AtomicReference<String> lastFinishedUrl = new AtomicReference<>();
    final AtomicReference<Runnable> pendingExtraction = new AtomicReference<>();
    final AtomicReference<Runnable> pendingTimeout = new AtomicReference<>();
    Runnable cleanup =
        () -> {
          if (!tornDown.compareAndSet(false, true)) {
            // teardown is idempotent; only the first terminal path runs it
            return;
          }
          Runnable extraction = pendingExtraction.get();
          if (extraction != null) {
            mainHandler.removeCallbacks(extraction);
          }
          Runnable timeout = pendingTimeout.get();
          if (timeout != null) {
            mainHandler.removeCallbacks(timeout);
          }
          try {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.clearCache(true);
            webView.clearFormData();
            webView.removeAllViews();
            webView.destroy();
            // Cookies are intentionally kept so a user-picked store context (D-022) survives
            // between lookups. "Forget store" wipes them through the shared
            // ForgetSelectedStoreUseCase -> WebViewStateClearer path.
          } catch (Exception ignored) {
            // best-effort teardown
          }
        };
    // Any terminal path, including caller cancellation, must tear the WebView down.
    future.whenComplete((result, error) -> mainHandler.post(cleanup));

    webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest navRequest) {
            String url = navRequest == null ? null : navRequest.getUrl().toString();
            if (isAllowedNavigation(url)) {
              return false;
            }
            // refuse to navigate off Walmart or to non-HTTPS schemes
            cleanup.run();
            if (!future.isDone()) {
              future.complete(
                  new StoreLookupResult.Error(
                      new AppError.UnknownError("Navigation outside Walmart is not allowed.")));
            }
            return true;
          }

          @Override
          public void onPageFinished(WebView view, String url) {
            if (future.isDone()) {
              return;
            }
            String previous = lastFinishedUrl.getAndSet(url);
            if (url != null && url.equals(previous)) {
              return;
            }
            // A redirect can finish more than one page. Re-arm extraction for each distinct final
            // URL so the lookup always reads the DOM the user actually landed on.
            Runnable pending = pendingExtraction.get();
            if (pending != null) {
              mainHandler.removeCallbacks(pending);
            }
            Runnable extraction =
                () -> {
                  pendingExtraction.set(null);
                  extract(view, request, future, cleanup);
                };
            pendingExtraction.set(extraction);
            mainHandler.postDelayed(extraction, EXTRACT_DELAY_MS);
          }

          @Override
          public void onReceivedHttpError(
              WebView view, WebResourceRequest navRequest, WebResourceResponse errorResponse) {
            if (errorResponse != null
                && errorResponse.getStatusCode() >= 400
                && navRequest != null
                && navRequest.isForMainFrame()) {
              int code = errorResponse.getStatusCode();
              if (code == 403 || code == 429 || code == 503) {
                if (!future.isDone()) {
                  future.complete(
                      new StoreLookupResult.Blocked(
                          "Walmart blocked the request (HTTP " + code + ")."));
                }
                cleanup.run();
              }
            }
          }

          @Override
          public void onReceivedError(
              WebView view, WebResourceRequest navRequest, WebResourceError error) {
            // ignore subresource failures; only a main-frame failure ends the lookup
            if (navRequest != null && !navRequest.isForMainFrame()) {
              return;
            }
            if (!future.isDone()) {
              String description =
                  error == null ? "WebView error" : String.valueOf(error.getDescription());
              future.complete(
                  new StoreLookupResult.Error(new AppError.NetworkUnavailable(description)));
            }
            cleanup.run();
          }
        });

    String q;
    try {
      q = URLEncoder.encode(request.getNormalizedText(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      q = request.getNormalizedText().replace(" ", "+");
    }
    String url =
        "https://www.walmart.com/search?q=" + q + "&facet=fulfillment_method_in_store:In-store";
    webView.loadUrl(url);

    Runnable timeout =
        () -> {
          pendingTimeout.set(null);
          if (!future.isDone()) {
            future.complete(
                new StoreLookupResult.Error(
                    new AppError.NetworkUnavailable("Walmart lookup timed out.")));
            cleanup.run();
          }
        };
    pendingTimeout.set(timeout);
    mainHandler.postDelayed(timeout, TIMEOUT_MS);
  }

  /** Visible for tests and the store picker: only HTTPS Walmart navigation is allowed. */
  public static boolean isAllowedNavigation(String url) {
    if (url == null) {
      return false;
    }
    if ("about:blank".equals(url)) {
      return true;
    }
    try {
      URI uri = new URI(url);
      if (!"https".equalsIgnoreCase(uri.getScheme())) {
        return false;
      }
      String host = uri.getHost();
      if (host == null) {
        return false;
      }
      host = host.toLowerCase(Locale.US);
      return host.equals("www.walmart.com") || host.equals("walmart.com");
    } catch (Exception e) {
      return false;
    }
  }

  private void extract(
      WebView webView,
      StoreLookupRequest request,
      CompletableFuture<StoreLookupResult> future,
      Runnable cleanup) {
    if (future.isDone()) {
      cleanup.run();
      return;
    }
    try {
      webView.evaluateJavascript(
          "(function(){"
              + "var s=document.getElementById('__NEXT_DATA__');"
              + "if(s&&s.textContent){return s.textContent;}"
              + "return document.documentElement ? document.documentElement.outerHTML : '';"
              + "})()",
          value -> {
            if (future.isDone()) {
              return;
            }
            // hand the JS payload to the worker pool; parsing MB-sized pages on the UI thread
            // causes visible jank
            executor.execute(
                () -> {
                  try {
                    String content = decodeJsString(value);
                    StoreLookupResult result =
                        downgradeUnverifiedStoreContext(
                            WalmartAisleExtractor.extractFromPage(content));
                    future.complete(result);
                  } catch (Exception e) {
                    future.complete(
                        new StoreLookupResult.Error(
                            new AppError.ParserFailed("Could not read Walmart page.")));
                  } finally {
                    mainHandler.post(cleanup);
                  }
                });
          });
    } catch (Exception e) {
      if (!future.isDone()) {
        future.complete(
            new StoreLookupResult.Error(new AppError.UnknownError("Walmart lookup failed.")));
      }
      cleanup.run();
    }
  }

  /** Downgrades extractor confidence when the retailer store could not be pinned. */
  static StoreLookupResult downgradeUnverifiedStoreContext(StoreLookupResult result) {
    if (result instanceof StoreLookupResult.Found found) {
      return new StoreLookupResult.Found(
          found.getAisle(),
          found.getBay(),
          found.getProductName(),
          found.getProductUrl(),
          Math.min(found.getConfidence() == null ? 0.5f : found.getConfidence(), 0.5f),
          found.getSource());
    }
    if (result instanceof StoreLookupResult.Multiple multiple) {
      List<StoreLookupResult.Found> candidates = new ArrayList<>();
      for (StoreLookupResult.Found c : multiple.getCandidates()) {
        candidates.add(
            new StoreLookupResult.Found(
                c.getAisle(),
                c.getBay(),
                c.getProductName(),
                c.getProductUrl(),
                Math.min(c.getConfidence() == null ? 0.5f : c.getConfidence(), 0.5f),
                c.getSource()));
      }
      return new StoreLookupResult.Multiple(candidates);
    }
    return result;
  }

  /**
   * Decodes the JSON string produced by {@code WebView.evaluateJavascript}. Returns an empty string
   * for a {@code null} JS value.
   */
  static String decodeJsString(String value) {
    if (value == null || "null".equals(value)) {
      return "";
    }
    try {
      return JsonParser.parseString(value).getAsString();
    } catch (Exception e) {
      throw new IllegalStateException("evaluateJavascript returned a non-JSON string", e);
    }
  }
}
