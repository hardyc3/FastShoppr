/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.homedepot;

import com.fastshoppr.app.data.remote.lookup.LookupConfig;
import com.fastshoppr.app.domain.error.AppError;
import com.fastshoppr.app.domain.lookup.StoreLookupRequest;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.lookup.StoreLookupService;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
public class HomeDepotHttpLookupService implements StoreLookupService {

  private final OkHttpClient client;
  private final LookupConfig config;
  private final ExecutorService executor;

  @Inject
  public HomeDepotHttpLookupService(
      OkHttpClient client, LookupConfig config, ExecutorService executor) {
    this.client = client;
    this.config = config;
    this.executor = executor;
  }

  @Override
  public StoreBrand getBrand() {
    return StoreBrand.HOME_DEPOT;
  }

  @Override
  public CompletableFuture<StoreLookupResult> lookup(StoreLookupRequest request) {
    if (!config.isHomeDepotLiveEnabled()) {
      // Dormant by default (D-021/D-022). Not a retailer block: mark as unsupported so the item
      // keeps manual entry and the rest of the run continues.
      return CompletableFuture.completedFuture(
          new StoreLookupResult.Error(
              new AppError.LookupNotFound(
                  "Home Depot lookup is not enabled yet; enter aisle manually.")));
    }
    return CompletableFuture.supplyAsync(() -> lookupLive(request), executor);
  }

  StoreLookupResult lookupLive(StoreLookupRequest request) {
    try {
      HttpUrl searchUrl =
          HttpUrl.parse(
              "https://www.homedepot.com/s/"
                  + URLEncoder.encode(request.getNormalizedText(), "UTF-8"));
      if (searchUrl == null) {
        return new StoreLookupResult.Error(new AppError.UnknownError("Invalid URL"));
      }
      Request req = new Request.Builder().url(searchUrl).get().build();
      try (Response response = client.newCall(req).execute()) {
        if (response.code() == 403 || response.code() == 429) {
          return new StoreLookupResult.Blocked(
              "Home Depot access blocked (HTTP " + response.code() + ").");
        }
        if (!response.isSuccessful() || response.body() == null) {
          return new StoreLookupResult.Error(
              new AppError.NetworkUnavailable("HTTP " + response.code()));
        }
        String body = response.body().string();
        // Minimal: parse search/result page directly. A fuller implementation would fetch up to 3
        // candidate product pages; parser supports product-page content regardless.
        return HomeDepotAisleParser.parseProductPage(
            body, searchUrl.toString(), request.getDisplayName());
      }
    } catch (IOException e) {
      return new StoreLookupResult.Error(new AppError.NetworkUnavailable(e.getMessage()));
    } catch (Exception e) {
      return new StoreLookupResult.Error(new AppError.UnknownError(e.getMessage()));
    }
  }
}
