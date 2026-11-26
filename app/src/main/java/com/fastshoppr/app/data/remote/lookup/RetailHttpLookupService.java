/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import com.fastshoppr.app.domain.error.AppError;
import com.fastshoppr.app.domain.lookup.StoreLookupRequest;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.lookup.StoreLookupService;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Public-page lookup that searches a retailer, then parses the first query-matching product page.
 *
 * <p>Choosing the first matching result is an explicit trade-off (D-055): it avoids a UI picker but
 * can resolve the wrong variant. Low-confidence results are downgraded by the repository, and
 * manual aisle entry stays available.
 */
public final class RetailHttpLookupService implements StoreLookupService {

  private final OkHttpClient client;
  private final ExecutorService executor;
  private final RateLimiter rateLimiter;
  private final LookupConfig config;
  private final RetailSiteConfig site;

  public RetailHttpLookupService(
      OkHttpClient client,
      ExecutorService executor,
      RateLimiter rateLimiter,
      LookupConfig config,
      RetailSiteConfig site) {
    this.client = client;
    this.executor = executor;
    this.rateLimiter = rateLimiter;
    this.config = config;
    this.site = site;
  }

  @Override
  public StoreBrand getBrand() {
    return site.brand;
  }

  @Override
  public CompletableFuture<StoreLookupResult> lookup(StoreLookupRequest request) {
    if (!config.isRetailHttpEnabled()) {
      return CompletableFuture.completedFuture(
          new StoreLookupResult.Error(
              new AppError.LookupNotFound(site.brand.getDisplayName() + " lookup is disabled.")));
    }
    return CompletableFuture.supplyAsync(() -> lookupLive(request), executor);
  }

  private StoreLookupResult lookupLive(StoreLookupRequest request) {
    String searchUrl = site.searchBaseUrl + encode(request.getNormalizedText());
    HttpUrl parsedSearch = HttpUrl.parse(searchUrl);
    if (parsedSearch == null) {
      return new StoreLookupResult.Error(new AppError.UnknownError("Invalid search URL"));
    }
    Page searchPage = fetch(parsedSearch.toString());
    StoreLookupResult failure = searchPage.failure();
    if (failure != null) {
      return failure;
    }
    if (RetailPageParser.isBlockedPage(searchPage.html())) {
      return new StoreLookupResult.Blocked(site.brand.getDisplayName() + " access blocked.");
    }
    // Search pages often start with unrelated carousel products. Match the user's query rather
    // than blindly taking document order; no overlap means no safe product to open (D-055).
    String productUrl =
        RetailPageParser.firstProductUrl(
            searchPage.html(),
            parsedSearch.toString(),
            site.host,
            site.productPathToken,
            request.getNormalizedText());
    if (productUrl == null) {
      return new StoreLookupResult.NotFound("No matching product page found");
    }
    // The repository reserves the first slot; this reserves the second request for the product
    // page.
    rateLimiter.acquire(site.brand);
    Page productPage = fetch(productUrl);
    failure = productPage.failure();
    if (failure != null) {
      return failure;
    }
    return RetailPageParser.parseProductPage(
        productPage.html(),
        productUrl,
        request.getDisplayName(),
        site.brand,
        site.source,
        request.getStore().getName(),
        request.getStore().getCity());
  }

  private Page fetch(String url) {
    Request request = new Request.Builder().url(url).get().build();
    try (Response response = client.newCall(request).execute()) {
      if (response.code() == 403 || response.code() == 429) {
        return Page.failed(
            new StoreLookupResult.Blocked(
                site.brand.getDisplayName() + " access blocked (HTTP " + response.code() + ")."));
      }
      if (!response.isSuccessful() || response.body() == null) {
        return Page.failed(
            new StoreLookupResult.Error(
                new AppError.NetworkUnavailable("HTTP " + response.code())));
      }
      return Page.success(response.body().string());
    } catch (IOException e) {
      return Page.failed(
          new StoreLookupResult.Error(new AppError.NetworkUnavailable(e.getMessage())));
    } catch (Exception e) {
      return Page.failed(new StoreLookupResult.Error(new AppError.UnknownError(e.getMessage())));
    }
  }

  private static String encode(String value) {
    // The Charset overload requires API 33; minSdk is 30.
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException impossible) {
      throw new IllegalStateException("UTF-8 is always available", impossible);
    }
  }

  private static final class Page {
    private final String html;
    private final StoreLookupResult failure;

    private Page(String html, StoreLookupResult failure) {
      this.html = html;
      this.failure = failure;
    }

    static Page success(String html) {
      return new Page(html, null);
    }

    static Page failed(StoreLookupResult failure) {
      return new Page(null, failure);
    }

    String html() {
      return html;
    }

    StoreLookupResult failure() {
      return failure;
    }
  }
}
