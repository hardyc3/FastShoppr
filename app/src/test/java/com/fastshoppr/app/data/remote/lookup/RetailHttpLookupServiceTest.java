/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.lookup.StoreLookupRequest;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.testing.FixedClock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

public class RetailHttpLookupServiceTest {

  @Test
  public void searchResultProductPageProducesFoundAisle() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      int[] productFetches = {0};
      OkHttpClient client =
          client(
              url -> {
                if (url.contains("/s/")) {
                  return fixture("homedepot-search.html");
                }
                productFetches[0]++;
                return fixture("../homedepot/product-aisle-bay.html");
              });
      RetailHttpLookupService service =
          new RetailHttpLookupService(
              client,
              executor,
              noWaitRateLimiter(),
              new LookupConfig(),
              RetailSiteConfig.HOME_DEPOT);

      StoreLookupResult result = service.lookup(request(StoreBrand.HOME_DEPOT, "milk")).join();

      assertTrue(result instanceof StoreLookupResult.Found);
      StoreLookupResult.Found found = (StoreLookupResult.Found) result;
      assertEquals("52", found.getAisle());
      assertEquals("16 oz. Steel Hammer", found.getProductName());
      assertEquals(LookupSource.HOME_DEPOT_HTTP, found.getSource());
      assertEquals("https://www.homedepot.com/p/Milk-Crate/204793156", found.getProductUrl());
      assertEquals(1, productFetches[0]);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void lowesSearchResultProductPageProducesFoundAisle() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      String[] searchUrl = {""};
      OkHttpClient client =
          client(
              url -> {
                if (url.contains("search?searchTerm=")) {
                  searchUrl[0] = url;
                  return fixture("lowes-search.html");
                }
                return fixture("lowes-product-aisle.html");
              });
      RetailHttpLookupService service =
          new RetailHttpLookupService(
              client, executor, noWaitRateLimiter(), new LookupConfig(), RetailSiteConfig.LOWES);

      StoreLookupResult result = service.lookup(request(StoreBrand.LOWES, "paper towels")).join();

      assertTrue(result instanceof StoreLookupResult.Found);
      assertEquals("14", ((StoreLookupResult.Found) result).getAisle());
      assertEquals("Moxie Ultra Paper Towels", ((StoreLookupResult.Found) result).getProductName());
      assertEquals(LookupSource.LOWES_HTTP, ((StoreLookupResult.Found) result).getSource());
      assertTrue(searchUrl[0].startsWith("https://www.lowes.com/search?searchTerm="));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void smithsSearchResultProductPageProducesFoundAisle() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      String[] searchUrl = {""};
      OkHttpClient client =
          client(
              url -> {
                if (url.contains("search?searchTerm=")) {
                  searchUrl[0] = url;
                  return fixture("smiths-search.html");
                }
                return fixture("smiths-product-aisle.html");
              });
      RetailHttpLookupService service =
          new RetailHttpLookupService(
              client, executor, noWaitRateLimiter(), new LookupConfig(), RetailSiteConfig.SMITHS);

      StoreLookupResult result = service.lookup(request(StoreBrand.SMITHS, "milk")).join();

      assertTrue(result instanceof StoreLookupResult.Found);
      assertEquals("A12", ((StoreLookupResult.Found) result).getAisle());
      assertEquals(
          "Smith's 1% Lowfat Milk Gallon", ((StoreLookupResult.Found) result).getProductName());
      assertEquals(0.9f, ((StoreLookupResult.Found) result).getConfidence(), 0.001f);
      assertEquals(LookupSource.SMITHS_HTTP, ((StoreLookupResult.Found) result).getSource());
      assertTrue(searchUrl[0].startsWith("https://www.smithsfoodanddrug.com/search?searchTerm="));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void productFetchReservesOneRateLimitSlot() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      int[] acquires = {0};
      RateLimiter limiter =
          new RateLimiter(new FixedClock(1L)) {
            @Override
            public void acquire(StoreBrand brand) {
              acquires[0]++;
            }
          };
      OkHttpClient client =
          client(
              url ->
                  url.contains("/s/")
                      ? fixture("homedepot-search.html")
                      : fixture("../homedepot/product-aisle-bay.html"));
      RetailHttpLookupService service =
          new RetailHttpLookupService(
              client, executor, limiter, new LookupConfig(), RetailSiteConfig.HOME_DEPOT);

      StoreLookupResult result = service.lookup(request(StoreBrand.HOME_DEPOT, "milk")).join();

      assertTrue(result instanceof StoreLookupResult.Found);
      // The repository reserves the search request; the service reserves the product request.
      assertEquals(1, acquires[0]);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void http403StopsBeforeProductFetch() throws Exception {
    assertBlockedForStatus(403);
  }

  @Test
  public void http429StopsBeforeProductFetch() throws Exception {
    assertBlockedForStatus(429);
  }

  @Test
  public void blockedSearchPageStopsBeforeProductFetch() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      int[] productFetches = {0};
      OkHttpClient client =
          client(
              url -> {
                if (url.contains("/search")) {
                  return fixture("../lowes/access-denied.html");
                }
                productFetches[0]++;
                return fixture("lowes-product-aisle.html");
              });
      RetailHttpLookupService service =
          new RetailHttpLookupService(
              client, executor, noWaitRateLimiter(), new LookupConfig(), RetailSiteConfig.LOWES);

      StoreLookupResult result = service.lookup(request(StoreBrand.LOWES, "paper towels")).join();

      assertTrue(result instanceof StoreLookupResult.Blocked);
      assertEquals(0, productFetches[0]);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void searchWithoutProductLinkReturnsNotFound() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      OkHttpClient client = client(url -> fixture("search-no-product.html"));
      RetailHttpLookupService service =
          new RetailHttpLookupService(
              client, executor, noWaitRateLimiter(), new LookupConfig(), RetailSiteConfig.SMITHS);

      StoreLookupResult result = service.lookup(request(StoreBrand.SMITHS, "milk")).join();

      assertTrue(result instanceof StoreLookupResult.NotFound);
    } finally {
      executor.shutdownNow();
    }
  }

  private static void assertBlockedForStatus(int statusCode) throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      int[] fetches = {0};
      OkHttpClient client =
          client(
              statusCode,
              url -> {
                fetches[0]++;
                return fixture("homedepot-search.html");
              });
      RetailHttpLookupService service =
          new RetailHttpLookupService(
              client,
              executor,
              noWaitRateLimiter(),
              new LookupConfig(),
              RetailSiteConfig.HOME_DEPOT);

      StoreLookupResult result = service.lookup(request(StoreBrand.HOME_DEPOT, "milk")).join();

      assertTrue(result instanceof StoreLookupResult.Blocked);
      assertEquals(1, fetches[0]);
    } finally {
      executor.shutdownNow();
    }
  }

  private static OkHttpClient client(PageSource source) {
    return client(200, source);
  }

  private static OkHttpClient client(int statusCode, PageSource source) {
    Interceptor interceptor =
        chain -> {
          String html = source.bodyFor(chain.request().url().toString());
          ResponseBody body = ResponseBody.create(html, MediaType.get("text/html"));
          return new Response.Builder()
              .request(chain.request())
              .protocol(Protocol.HTTP_1_1)
              .code(statusCode)
              .message(statusCode == 200 ? "OK" : "Blocked")
              .body(body)
              .build();
        };
    return new OkHttpClient.Builder().addInterceptor(interceptor).build();
  }

  private static RateLimiter noWaitRateLimiter() {
    return new RateLimiter(new FixedClock(1L)) {
      @Override
      public void acquire(StoreBrand brand) {}
    };
  }

  private static StoreLookupRequest request(StoreBrand brand, String query) {
    Store store =
        new Store(
            "store-" + brand,
            brand,
            brand.getDisplayName(),
            null,
            "Payson",
            null,
            null,
            null,
            null,
            false,
            true,
            false,
            0L,
            0L);
    return new StoreLookupRequest(query, query, query, store);
  }

  private static String fixture(String name) throws IOException {
    Path path = Path.of("src/test/resources/fixtures/retail", name);
    if (!Files.exists(path)) {
      path = Path.of("app/src/test/resources/fixtures/retail", name);
    }
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  @FunctionalInterface
  private interface PageSource {
    String bodyFor(String url) throws IOException;
  }
}
