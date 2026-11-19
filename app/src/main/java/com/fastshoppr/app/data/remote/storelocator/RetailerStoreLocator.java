/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.storelocator;

import com.fastshoppr.app.data.remote.lookup.RateLimiter;
import com.fastshoppr.app.domain.model.GeoPoint;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorBlockedException;
import com.fastshoppr.app.domain.repository.StoreDirectoryRepository;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Fetches public retailer store-locator pages and extracts branch candidates. */
@Singleton
public final class RetailerStoreLocator implements StoreDirectoryRepository {

  private static final String USER_AGENT =
      "FastShoppr/1.0 (Android; local shopping-list app; store-locator lookup)";
  private static final String BLOCK_MESSAGE =
      "The store locator blocked this request. Enter your store manually.";

  private final OkHttpClient client;
  private final RateLimiter rateLimiter;
  private final StoreLocatorCache cache;
  private final ExecutorService executor;

  @Inject
  public RetailerStoreLocator(
      OkHttpClient client,
      RateLimiter rateLimiter,
      StoreLocatorCache cache,
      ExecutorService executor) {
    this.client = client;
    this.rateLimiter = rateLimiter;
    this.cache = cache;
    this.executor = executor;
  }

  @Override
  public CompletableFuture<List<NearbyStore>> findNearby(
      StoreBrand brand, GeoPoint origin, String postalCode) {
    if (brand == null || brand == StoreBrand.CUSTOM) {
      return failedFuture(new IllegalArgumentException("A supported brand is required"));
    }
    if (origin == null && (postalCode == null || postalCode.isBlank())) {
      return failedFuture(new IllegalArgumentException("A location or postal code is required"));
    }
    final String cacheKey = cacheKey(brand, origin, postalCode);
    return CompletableFuture.supplyAsync(
        () -> {
          List<NearbyStore> cached = cache.get(cacheKey);
          if (cached != null && !cached.isEmpty()) {
            return cached;
          }
          rateLimiter.acquire(brand);
          String html = fetch(urlFor(brand, origin, postalCode));
          if (isBlocked(html)) {
            throw new StoreLocatorBlockedException(BLOCK_MESSAGE);
          }
          List<NearbyStore> stores = StoreLocatorParser.parse(html, brand);
          if (!stores.isEmpty()) {
            cache.put(cacheKey, stores);
          }
          return stores;
        },
        executor);
  }

  private static CompletableFuture<List<NearbyStore>> failedFuture(Throwable error) {
    CompletableFuture<List<NearbyStore>> failed = new CompletableFuture<>();
    failed.completeExceptionally(error);
    return failed;
  }

  private String fetch(String url) {
    Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
    try (Response response = client.newCall(request).execute()) {
      if (response.code() == 403 || response.code() == 429) {
        throw new StoreLocatorBlockedException(BLOCK_MESSAGE);
      }
      if (!response.isSuccessful()) {
        throw new IllegalStateException("Store locator returned HTTP " + response.code());
      }
      ResponseBody body = response.body();
      return body == null ? "" : body.string();
    } catch (IOException error) {
      throw new IllegalStateException("Could not reach the store locator. Try again later.", error);
    }
  }

  private static boolean isBlocked(String html) {
    if (html == null || html.isBlank()) {
      return false;
    }
    String lower = html.toLowerCase(Locale.US);
    return lower.contains("robot or human")
        || lower.contains("captcha")
        || lower.contains("access denied")
        || lower.contains("bot challenge");
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException impossible) {
      throw new IllegalStateException("UTF-8 is not available", impossible);
    }
  }

  private static String cacheKey(StoreBrand brand, GeoPoint origin, String postalCode) {
    if (origin != null) {
      int latCell = (int) Math.round(origin.getLatitude() * 100.0);
      int lonCell = (int) Math.round(origin.getLongitude() * 100.0);
      return brand.name() + ":coarse:" + coarseCellToken(latCell, lonCell);
    }
    return brand.name() + ":zip:" + postalCode.trim().toLowerCase(Locale.US);
  }

  private static String coarseCellToken(int latCell, int lonCell) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((latCell + ":" + lonCell).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is not available", impossible);
    }
  }

  private static String urlFor(StoreBrand brand, GeoPoint origin, String postalCode) {
    String query;
    if (origin != null) {
      query = String.format(Locale.US, "%.2f,%.2f", origin.getLatitude(), origin.getLongitude());
    } else {
      query = postalCode == null ? "" : postalCode.trim();
    }
    String encoded = encode(query);
    return switch (brand) {
      case WALMART -> "https://www.walmart.com/store/finder?location=" + encoded;
      case HOME_DEPOT ->
          origin != null
              ? "https://www.homedepot.com/l/store-locator?lat="
                  + String.format(Locale.US, "%.2f", origin.getLatitude())
                  + "&lng="
                  + String.format(Locale.US, "%.2f", origin.getLongitude())
              : "https://www.homedepot.com/l/store-locator?zip=" + encoded;
      case LOWES -> "https://www.lowes.com/store?query=" + encoded;
      case TARGET -> "https://www.target.com/store-locator/find-stores?search=" + encoded;
      case SMITHS -> "https://www.smithsfoodanddrug.com/stores?query=" + encoded;
      case MACYS -> "https://www.macys.com/stores?q=" + encoded;
      case CUSTOM -> throw new IllegalArgumentException("Custom stores have no public locator");
    };
  }
}
