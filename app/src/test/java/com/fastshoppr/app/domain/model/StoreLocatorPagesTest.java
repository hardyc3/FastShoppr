/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public final class StoreLocatorPagesTest {

  private static final List<StoreBrand> ORDER =
      List.of(StoreBrand.WALMART, StoreBrand.HOME_DEPOT, StoreBrand.LOWES, StoreBrand.SMITHS);

  @Test
  public void supportedBrandsMatchStableBrandOrder() {
    assertEquals(ORDER, StoreLocatorPages.brandOrder());
    assertEquals(Set.copyOf(ORDER), StoreLocatorPages.supportedBrands());
    assertFalse(StoreLocatorPages.supportedBrands().contains(StoreBrand.CUSTOM));
    assertFalse(StoreLocatorPages.supports(null));
    assertFalse(StoreLocatorPages.supports(StoreBrand.CUSTOM));
    for (StoreBrand brand : ORDER) {
      assertTrue(StoreLocatorPages.supports(brand));
    }
  }

  @Test
  public void everySupportedBrandHasAnHttpsLocatorUrl() {
    for (StoreBrand brand : ORDER) {
      String url = StoreLocatorPages.locatorUrl(brand);
      assertTrue(url, url.startsWith("https://"));
      assertTrue(url, StoreLocatorPages.isAllowedNavigation(brand, url));
    }
    assertEquals(
        "https://www.walmart.com/store/finder", StoreLocatorPages.locatorUrl(StoreBrand.WALMART));
    assertEquals(
        "https://www.homedepot.com/l/store-locator",
        StoreLocatorPages.locatorUrl(StoreBrand.HOME_DEPOT));
    assertEquals("https://www.lowes.com/store", StoreLocatorPages.locatorUrl(StoreBrand.LOWES));
    assertEquals(
        "https://www.smithsfoodanddrug.com/stores/search",
        StoreLocatorPages.locatorUrl(StoreBrand.SMITHS));
  }

  @Test
  public void unsupportedBrandHasNoLocatorUrl() {
    assertThrows(
        IllegalArgumentException.class, () -> StoreLocatorPages.locatorUrl(StoreBrand.CUSTOM));
    assertThrows(IllegalArgumentException.class, () -> StoreLocatorPages.locatorUrl(null));
  }

  @Test
  public void navigationAllowlistRequiresHttpsOnTheExactRetailerHost() {
    assertTrue(StoreLocatorPages.isAllowedNavigation(StoreBrand.WALMART, "about:blank"));
    for (StoreBrand brand : ORDER) {
      String host = URI.create(StoreLocatorPages.locatorUrl(brand)).getHost();
      assertTrue(StoreLocatorPages.isAllowedNavigation(brand, "https://" + host + "/stores"));
      assertTrue(StoreLocatorPages.isAllowedNavigation(brand, "https://www." + host + "/stores"));
      assertFalse(StoreLocatorPages.isAllowedNavigation(brand, "http://" + host + "/stores"));
      assertFalse(
          StoreLocatorPages.isAllowedNavigation(brand, "https://" + host + ".evil.example/stores"));
      assertFalse(
          StoreLocatorPages.isAllowedNavigation(brand, "https://evil.example/?next=" + host));
      assertFalse(StoreLocatorPages.isAllowedNavigation(brand, "javascript:alert(1)"));
      assertFalse(StoreLocatorPages.isAllowedNavigation(brand, "not a url"));
      assertFalse(StoreLocatorPages.isAllowedNavigation(brand, null));
    }
  }

  @Test
  public void navigationToAnotherSupportedRetailerIsRejected() {
    assertFalse(
        StoreLocatorPages.isAllowedNavigation(
            StoreBrand.WALMART, StoreLocatorPages.locatorUrl(StoreBrand.LOWES)));
    assertFalse(
        StoreLocatorPages.isAllowedNavigation(
            StoreBrand.HOME_DEPOT, StoreLocatorPages.locatorUrl(StoreBrand.SMITHS)));
  }

  @Test
  public void challengePagesAreDetectedFromTitleOrBody() {
    assertTrue(StoreLocatorPages.isChallengePage("Robot or human?", ""));
    assertTrue(StoreLocatorPages.isChallengePage("Store", "Are you a human?"));
    assertTrue(StoreLocatorPages.isChallengePage("", "Please complete the bot check"));
    assertTrue(StoreLocatorPages.isChallengePage("", "ACCESS DENIED"));
    assertTrue(StoreLocatorPages.isChallengePage("Request Blocked", ""));
    assertFalse(
        StoreLocatorPages.isChallengePage("Walmart #123", "Store details, aisles and hours"));
    assertFalse(StoreLocatorPages.isChallengePage(null, null));
  }
}
