/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.storelocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fastshoppr.app.data.remote.lookup.RateLimiter;
import com.fastshoppr.app.domain.model.GeoPoint;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorBlockedException;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public final class RetailerStoreLocatorTest {

  @Test
  public void fetchesAndParsesPublicLocatorPage() throws Exception {
    String html =
        """
        <script type="application/ld+json">
        {"name":"Walmart Supercenter #123","identifier":"123",
         "address":{"streetAddress":"1 Main St","addressLocality":"Springfield",
                    "addressRegion":"IL","postalCode":"62701"},
         "geo":{"latitude":39.8,"longitude":-89.6}}
        </script>
        """;
    AtomicReference<String> requestedUrl = new AtomicReference<>();
    AtomicReference<String> userAgent = new AtomicReference<>();
    StoreLocatorCache cache = mock(StoreLocatorCache.class);
    RetailerStoreLocator locator = locatorFor(html, 200, cache, requestedUrl, userAgent);

    List<NearbyStore> stores =
        locator.findNearby(StoreBrand.WALMART, new GeoPoint(39.789123, -89.641234), null).get();

    assertEquals(1, stores.size());
    assertEquals("123", stores.get(0).getId());
    assertTrue(requestedUrl.get().contains("walmart.com/store/finder"));
    assertTrue(requestedUrl.get().contains("39.79%2C-89.64"));
    assertTrue(userAgent.get().contains("FastShoppr"));
    ArgumentCaptor<String> cacheKey = ArgumentCaptor.forClass(String.class);
    verify(cache).put(cacheKey.capture(), anyList());
    assertFalse(cacheKey.getValue().contains("coarse:3979:-8964"));
  }

  @Test
  public void blockedResponseFailsWithClearMessage() {
    StoreLocatorCache cache = mock(StoreLocatorCache.class);
    RetailerStoreLocator locator =
        locatorFor("", 403, cache, new AtomicReference<>(), new AtomicReference<>());

    ExecutionException error =
        assertThrows(
            ExecutionException.class,
            () -> locator.findNearby(StoreBrand.WALMART, null, "90210").get());

    assertTrue(error.getCause() instanceof StoreLocatorBlockedException);
    assertTrue(error.getCause().getMessage().contains("blocked"));
    verify(cache, never()).put(anyString(), anyList());
  }

  @Test
  public void tooManyRequestsFailsWithBlockedException() {
    StoreLocatorCache cache = mock(StoreLocatorCache.class);
    RetailerStoreLocator locator =
        locatorFor("", 429, cache, new AtomicReference<>(), new AtomicReference<>());

    ExecutionException error =
        assertThrows(
            ExecutionException.class,
            () -> locator.findNearby(StoreBrand.WALMART, null, "90210").get());

    assertTrue(error.getCause() instanceof StoreLocatorBlockedException);
    verify(cache, never()).put(anyString(), anyList());
  }

  @Test
  public void botChallengeBodyFailsWithBlockedException() {
    StoreLocatorCache cache = mock(StoreLocatorCache.class);
    RetailerStoreLocator locator =
        locatorFor(
            "<html><body><h1>Robot or human?</h1></body></html>",
            200,
            cache,
            new AtomicReference<>(),
            new AtomicReference<>());

    ExecutionException error =
        assertThrows(
            ExecutionException.class,
            () -> locator.findNearby(StoreBrand.TARGET, null, "90210").get());

    assertTrue(error.getCause() instanceof StoreLocatorBlockedException);
    verify(cache, never()).put(anyString(), anyList());
  }

  private static RetailerStoreLocator locatorFor(
      String body,
      int statusCode,
      StoreLocatorCache cache,
      AtomicReference<String> requestedUrl,
      AtomicReference<String> userAgent) {
    Interceptor interceptor =
        chain -> {
          requestedUrl.set(chain.request().url().toString());
          userAgent.set(chain.request().header("User-Agent"));
          return response(statusCode, body);
        };
    OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
    return new RetailerStoreLocator(
        client, new RateLimiter(() -> System.currentTimeMillis()), cache, DIRECT_EXECUTOR);
  }

  private static final AbstractExecutorService DIRECT_EXECUTOR =
      new AbstractExecutorService() {
        @Override
        public void shutdown() {}

        @Override
        public List<Runnable> shutdownNow() {
          return List.of();
        }

        @Override
        public boolean isShutdown() {
          return false;
        }

        @Override
        public boolean isTerminated() {
          return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
          return true;
        }

        @Override
        public void execute(Runnable command) {
          command.run();
        }
      };

  private static Response response(int code, String body) {
    return new Response.Builder()
        .request(new Request.Builder().url("https://example.com").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(code == 200 ? "OK" : "Blocked")
        .body(ResponseBody.create(MediaType.get("text/html; charset=utf-8"), body))
        .build();
  }
}
