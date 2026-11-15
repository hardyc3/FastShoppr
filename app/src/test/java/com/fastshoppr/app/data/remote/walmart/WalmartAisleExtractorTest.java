/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.walmart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    assertTrue(result instanceof StoreLookupResult.Found);
    StoreLookupResult.Found found = (StoreLookupResult.Found) result;
    assertEquals("AC4", found.getAisle());
  }

  @Test
  public void normalPageWithPerimeterxAllowlistIsNotBlocked() throws Exception {
    String html = fixture("normal-page-with-px-allowlist.html");
    StoreLookupResult result = WalmartAisleExtractor.extractFromPage(html);
    assertTrue(result instanceof StoreLookupResult.Found);
    assertEquals("A29", ((StoreLookupResult.Found) result).getAisle());
  }

  @Test
  public void detectsBlockedChallenge() throws Exception {
    String html = fixture("blocked.html");
    StoreLookupResult result = WalmartAisleExtractor.extractFromPage(html);
    assertTrue(result instanceof StoreLookupResult.Blocked);
  }

  @Test
  public void realSearchPageWithoutAisleReturnsNotFoundNotBlocked() throws Exception {
    String json = fixture("next-data-no-aisle.json");
    String html = "<html><script id=\"__NEXT_DATA__\">" + json + "</script></html>";
    StoreLookupResult result = WalmartAisleExtractor.extractFromPage(html);
    assertTrue(result instanceof StoreLookupResult.NotFound);
    assertFalse(result instanceof StoreLookupResult.Blocked);
  }

  @Test
  public void mostCommonAisleWins() throws Exception {
    // J23 appears twice, H19 once -> J23 is the most common aisle
    String json = fixture("next-data-real-aisle-j23.json");
    StoreLookupResult result = WalmartAisleExtractor.extractFromPage(json);
    assertTrue(result instanceof StoreLookupResult.Found);
    StoreLookupResult.Found found = (StoreLookupResult.Found) result;
    assertEquals("J23", found.getAisle());
    // confidence = dominance ratio 2/3, capped at 0.9
    assertTrue(found.getConfidence() >= 0.6f && found.getConfidence() <= 0.7f);
  }

  @Test
  public void tieBreaksToFirstAisleSeen() throws Exception {
    // j23 appears first, h1 second; both count 1 -> j23 wins tie
    String json = fixture("next-data-tiebreak.json");
    StoreLookupResult result = WalmartAisleExtractor.extractFromPage(json);
    assertTrue(result instanceof StoreLookupResult.Found);
    StoreLookupResult.Found found = (StoreLookupResult.Found) result;
    assertEquals("J23", found.getAisle());
  }

  private static String fixture(String name) throws IOException {
    Path path = Path.of("src/test/resources/fixtures/walmart", name);
    if (!Files.exists(path)) {
      path = Path.of("app/src/test/resources/fixtures/walmart", name);
    }
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}
