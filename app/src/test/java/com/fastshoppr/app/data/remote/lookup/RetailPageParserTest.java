/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class RetailPageParserTest {

  @Test
  public void firstProductUrlPicksFirstMatchingRetailProductLink() throws Exception {
    String html = fixture("homedepot-search.html");

    assertEquals(
        "https://www.homedepot.com/p/Milk-Crate/204793156",
        RetailPageParser.firstProductUrl(
            html, "https://www.homedepot.com/s/milk", "homedepot.com", "/p/"));
  }

  @Test
  public void firstProductUrlIgnoresOffHostLinks() throws Exception {
    String html = fixture("homedepot-search.html");

    assertNull(
        RetailPageParser.firstProductUrl(
            html, "https://www.homedepot.com/s/milk", "example.test", "/p/"));
  }

  @Test
  public void firstProductUrlRequiresHttps() throws Exception {
    String html =
        "<html><body><a href=\"http://www.homedepot.com/p/Unsafe/1\">Unsafe</a>"
            + "<a href=\"https://www.homedepot.com/p/Safe/2\">Safe</a></body></html>";

    assertEquals(
        "https://www.homedepot.com/p/Safe/2",
        RetailPageParser.firstProductUrl(
            html, "https://www.homedepot.com/s/milk", "homedepot.com", "/p/"));
  }

  @Test
  public void queryAwareFirstProductUrlSkipsUnrelatedCarousel() throws Exception {
    String html = fixture("search-carousel.html");

    assertEquals(
        "https://www.homedepot.com/p/Milk-Crate/204793156",
        RetailPageParser.firstProductUrl(
            html, "https://www.homedepot.com/s/milk", "homedepot.com", "/p/", "milk"));
  }

  @Test
  public void queryAwareFirstProductUrlSkipsSmithsCarouselProducts() throws Exception {
    String html = fixture("smiths-search.html");

    assertEquals(
        "https://www.smithsfoodanddrug.com/p/smith-s-1-lowfat-milk-gallon/0007042400071",
        RetailPageParser.firstProductUrl(
            html,
            "https://www.smithsfoodanddrug.com/search?searchTerm=milk",
            "smithsfoodanddrug.com",
            "/p/",
            "milk"));
  }

  @Test
  public void queryAwareFirstProductUrlReturnsNullWithoutOverlap() throws Exception {
    String html = fixture("smiths-search.html");

    assertNull(
        RetailPageParser.firstProductUrl(
            html,
            "https://www.smithsfoodanddrug.com/search?searchTerm=batteries",
            "smithsfoodanddrug.com",
            "/p/",
            "batteries"));
  }

  @Test
  public void queryAwareFirstProductUrlKeepsFirstMatchInsteadOfBestOverlap() {
    String html =
        "<html><body>"
            + "<a href=\"https://www.homedepot.com/p/Milk/1\">Milk</a>"
            + "<a href=\"https://www.homedepot.com/p/Organic-Whole-Milk-Gallon/2\">"
            + "Organic Whole Milk Gallon</a>"
            + "</body></html>";

    assertEquals(
        "https://www.homedepot.com/p/Milk/1",
        RetailPageParser.firstProductUrl(
            html, "https://www.homedepot.com/s/milk", "homedepot.com", "/p/", "milk"));
  }

  @Test
  public void aisleForDifferentStoreIsNotInferred() throws Exception {
    String html = "<html><body><p>Available at Other Town: Aisle B2</p></body></html>";

    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            html,
            "https://www.smithsfoodanddrug.com/p/milk/1",
            "Milk",
            StoreBrand.SMITHS,
            LookupSource.SMITHS_HTTP,
            "Smiths",
            "Payson");

    assertTrue(result instanceof StoreLookupResult.NotFound);
  }

  @Test
  public void genericAisleWithoutProductTitleIsLowConfidence() throws Exception {
    String html = "<html><body><p>Aisle C3</p></body></html>";

    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            html,
            "https://www.homedepot.com/p/item/1",
            "User milk",
            StoreBrand.HOME_DEPOT,
            LookupSource.HOME_DEPOT_HTTP,
            "Home Depot",
            "Payson");

    assertTrue(result instanceof StoreLookupResult.Found);
    StoreLookupResult.Found found = (StoreLookupResult.Found) result;
    assertEquals("C3", found.getAisle());
    assertEquals("User milk", found.getProductName());
    assertTrue(found.getConfidence() < 0.6f);
  }

  @Test
  public void parsesAisleAndBayForHomeDepotProductPage() throws Exception {
    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            fixture("../homedepot/product-aisle-bay.html"),
            "https://www.homedepot.com/p/Milk-Crate/204793156",
            "Milk Crate",
            StoreBrand.HOME_DEPOT,
            LookupSource.HOME_DEPOT_HTTP);

    assertTrue(result instanceof StoreLookupResult.Found);
    StoreLookupResult.Found found = (StoreLookupResult.Found) result;
    assertEquals("52", found.getAisle());
    assertEquals("016", found.getBay());
    assertEquals(LookupSource.HOME_DEPOT_HTTP, found.getSource());
  }

  @Test
  public void parsesVisibleAisleForLowesProductPage() throws Exception {
    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            fixture("lowes-product-aisle.html"),
            "https://www.lowes.com/pd/Paper-Towels/5015563337",
            "Paper Towels",
            StoreBrand.LOWES,
            LookupSource.LOWES_HTTP);

    assertTrue(result instanceof StoreLookupResult.Found);
    StoreLookupResult.Found found = (StoreLookupResult.Found) result;
    assertEquals("14", found.getAisle());
    assertEquals(LookupSource.LOWES_HTTP, found.getSource());
  }

  @Test
  public void parsesVisibleAisleForSmithsProductPage() throws Exception {
    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            fixture("smiths-product-aisle.html"),
            "https://www.smithsfoodanddrug.com/p/milk/0007042400071",
            "Milk",
            StoreBrand.SMITHS,
            LookupSource.SMITHS_HTTP);

    assertTrue(result instanceof StoreLookupResult.Found);
    assertEquals("A12", ((StoreLookupResult.Found) result).getAisle());
  }

  @Test
  public void returnsNotFoundForProductPageWithoutAisle() throws Exception {
    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            fixture("../smiths/product-no-public-aisle.html"),
            "https://www.smithsfoodanddrug.com/p/milk/0007042400071",
            "Milk",
            StoreBrand.SMITHS,
            LookupSource.SMITHS_HTTP);

    assertTrue(result instanceof StoreLookupResult.NotFound);
  }

  @Test
  public void punctuationOnlyAisleMatchIsIgnored() throws Exception {
    String html = "<html><body><p>Shop the apple aisle. More items below.</p></body></html>";

    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            html,
            "https://www.smithsfoodanddrug.com/p/milk/1",
            "Milk",
            StoreBrand.SMITHS,
            LookupSource.SMITHS_HTTP);

    assertTrue(result instanceof StoreLookupResult.NotFound);
  }

  @Test
  public void detectsHomeDepotErrorPageAsBlocked() throws Exception {
    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            fixture("homedepot-search-error.html"),
            "https://www.homedepot.com/s/milk",
            "milk",
            StoreBrand.HOME_DEPOT,
            LookupSource.HOME_DEPOT_HTTP);

    assertTrue(result instanceof StoreLookupResult.Blocked);
  }

  @Test
  public void detectsBlockedPageBeforeReadingAisleText() throws Exception {
    StoreLookupResult result =
        RetailPageParser.parseProductPage(
            fixture("../lowes/access-denied.html"),
            "https://www.lowes.com/pd/Paper-Towels/5015563337",
            "Paper Towels",
            StoreBrand.LOWES,
            LookupSource.LOWES_HTTP);

    assertTrue(result instanceof StoreLookupResult.Blocked);
  }

  private static String fixture(String name) throws IOException {
    Path path = Path.of("src/test/resources/fixtures/retail", name);
    if (!Files.exists(path)) {
      path = Path.of("app/src/test/resources/fixtures/retail", name);
    }
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}
