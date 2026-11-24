/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.di;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import okhttp3.Request;
import org.junit.Test;

public final class NetworkModuleTest {

  @Test
  public void keepsCallerSuppliedUserAgent() {
    Request request =
        new Request.Builder()
            .url("https://example.com")
            .header("User-Agent", "FastShoppr/1.0 (store locator)")
            .build();

    Request result = NetworkModule.withDefaultHeaders(request);

    assertEquals("FastShoppr/1.0 (store locator)", result.header("User-Agent"));
    assertEquals("en-US,en;q=0.9", result.header("Accept-Language"));
  }

  @Test
  public void addsDefaultUserAgentWhenAbsent() {
    Request request = new Request.Builder().url("https://example.com").build();

    Request result = NetworkModule.withDefaultHeaders(request);

    assertTrue(result.header("User-Agent").contains("FastShoppr"));
  }
}
