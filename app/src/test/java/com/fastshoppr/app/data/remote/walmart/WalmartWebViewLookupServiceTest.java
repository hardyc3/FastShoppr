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
import com.fastshoppr.app.domain.model.LookupSource;
import java.util.List;
import org.junit.Test;

public class WalmartWebViewLookupServiceTest {

  @Test
  public void decodesEvaluateJavascriptPayload() {
    String decoded =
        WalmartWebViewLookupService.decodeJsString(
            "\"{\\\"Location\\\":[{\\\"displayValue\\\":\\\"A29\\\"}]}\"");
    assertEquals("{\"Location\":[{\"displayValue\":\"A29\"}]}", decoded);
  }

  @Test
  public void decodesNullJavascriptValueAsEmpty() {
    assertEquals("", WalmartWebViewLookupService.decodeJsString(null));
    assertEquals("", WalmartWebViewLookupService.decodeJsString("null"));
  }

  @Test(expected = IllegalStateException.class)
  public void decodeRejectsNonJsonPayload() {
    WalmartWebViewLookupService.decodeJsString("not json");
  }

  @Test
  public void allowsOnlyWalmartHttpsNavigation() {
    assertTrue(WalmartWebViewLookupService.isAllowedNavigation("https://www.walmart.com/p/1"));
    assertTrue(WalmartWebViewLookupService.isAllowedNavigation("https://walmart.com/"));
    assertTrue(WalmartWebViewLookupService.isAllowedNavigation("about:blank"));
    assertFalse(WalmartWebViewLookupService.isAllowedNavigation("http://www.walmart.com/"));
    assertFalse(WalmartWebViewLookupService.isAllowedNavigation("https://evil.example/"));
    assertFalse(WalmartWebViewLookupService.isAllowedNavigation("file:///data/data/x"));
    assertFalse(WalmartWebViewLookupService.isAllowedNavigation("javascript:alert(1)"));
    assertFalse(WalmartWebViewLookupService.isAllowedNavigation(null));
  }

  @Test
  public void downgradeUnverifiedFoundBelowReviewThreshold() {
    StoreLookupResult result =
        new StoreLookupResult.Found("A29", null, "Milk", null, 0.9f, LookupSource.WALMART_WEBVIEW);
    StoreLookupResult downgraded =
        WalmartWebViewLookupService.downgradeUnverifiedStoreContext(result);
    StoreLookupResult.Found found = (StoreLookupResult.Found) downgraded;
    assertTrue(found.getConfidence() < 0.6f);
    assertEquals("A29", found.getAisle());
  }

  @Test
  public void downgradeUnverifiedMultipleKeepsCandidates() {
    StoreLookupResult result =
        new StoreLookupResult.Multiple(
            List.of(
                new StoreLookupResult.Found(
                    "A29", null, "Milk", null, 0.9f, LookupSource.WALMART_WEBVIEW)));
    StoreLookupResult downgraded =
        WalmartWebViewLookupService.downgradeUnverifiedStoreContext(result);
    StoreLookupResult.Multiple multiple = (StoreLookupResult.Multiple) downgraded;
    assertEquals(1, multiple.getCandidates().size());
    assertTrue(multiple.getCandidates().get(0).getConfidence() < 0.6f);
  }

  @Test
  public void downgradeLeavesNonFoundUntouched() {
    StoreLookupResult blocked = new StoreLookupResult.Blocked("blocked");
    assertEquals(blocked, WalmartWebViewLookupService.downgradeUnverifiedStoreContext(blocked));
    StoreLookupResult notFound = new StoreLookupResult.NotFound("no");
    assertEquals(notFound, WalmartWebViewLookupService.downgradeUnverifiedStoreContext(notFound));
  }
}
