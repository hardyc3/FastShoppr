/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.walmart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class WalmartAisleExtractorTest {

  @Test
  public void extractsAisleFromNextData() throws Exception {
    String json = fixture("next-data-aisle-a29.json");
    String html = "<html><script id=\"__NEXT_DATA__\">" + json + "</script></html>";
    StoreLookupResult result = WalmartAisleExtractor.extractFromPage(html);
    assertTrue(result instanceof StoreLookupResult.Found);
    StoreLookupResult.Found found = (StoreLookupResult.Found) result;
    assertEquals("A29", found.getAisle());
  }

  @Test
  public void returnsMultipleCandidates() throws Exception {
    String json = fixture("next-data-multiple.json");
    StoreLookupResult result = WalmartAisleExtractor.extractFromPage(json);
    assertTrue(result instanceof StoreLookupResult.Multiple);
    StoreLookupResult.Multiple multiple = (StoreLookupResult.Multiple) result;
    assertTrue(multiple.getCandidates().size() >= 2);
  }

  @Test
  public void detectsBlockedChallenge() throws Exception {
    String html = fixture("blocked.html");
    StoreLookupResult result = WalmartAisleExtractor.extractFromPage(html);
    assertTrue(result instanceof StoreLookupResult.Blocked);
  }

  private static String fixture(String name) throws IOException {
    Path path = Path.of("src/test/resources/fixtures/walmart", name);
    if (!Files.exists(path)) {
      path = Path.of("app/src/test/resources/fixtures/walmart", name);
    }
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}
