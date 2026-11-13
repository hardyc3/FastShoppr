/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.homedepot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class HomeDepotAisleParserTest {

  @Test
  public void parsesAisleAndBay() throws Exception {
    String html = fixture("product-aisle-bay.html");
    StoreLookupResult result =
        HomeDepotAisleParser.parseProductPage(html, "https://example.test/p/1", null);
    assertTrue(result instanceof StoreLookupResult.Found);
    StoreLookupResult.Found found = (StoreLookupResult.Found) result;
    assertEquals("52", found.getAisle());
    assertEquals("016", found.getBay());
  }

  @Test
  public void returnsNotFoundWhenNoAisle() throws Exception {
    String html = fixture("product-no-aisle.html");
    StoreLookupResult result =
        HomeDepotAisleParser.parseProductPage(html, "https://example.test/p/2", "Item");
    assertTrue(result instanceof StoreLookupResult.NotFound);
  }

  @Test
  public void detectsBlockedPage() throws Exception {
    String html = fixture("blocked.html");
    StoreLookupResult result =
        HomeDepotAisleParser.parseProductPage(html, "https://example.test/p/3", "Item");
    assertTrue(result instanceof StoreLookupResult.Blocked);
  }

  private static String fixture(String name) throws IOException {
    Path path = Path.of("src/test/resources/fixtures/homedepot", name);
    if (!Files.exists(path)) {
      path = Path.of("app/src/test/resources/fixtures/homedepot", name);
    }
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}
