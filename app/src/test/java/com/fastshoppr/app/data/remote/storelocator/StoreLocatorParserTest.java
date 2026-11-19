/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.storelocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.util.List;
import org.junit.Test;

public final class StoreLocatorParserTest {

  @Test
  public void parsesJsonLdLocalBusinessEntries() {
    String html =
        """
        <html><body>
        <script type="application/ld+json">
        {"@context":"https://schema.org","@graph":[
          {"@type":"LocalBusiness","name":"Walmart Supercenter #123","identifier":"123",
           "address":{"streetAddress":"1 Main St","addressLocality":"Springfield",
                      "addressRegion":"IL","postalCode":"62701"},
           "geo":{"latitude":39.8,"longitude":-89.6}},
          {"@type":"LocalBusiness","name":"Walmart #456","storeNumber":"456",
           "addressLine1":"2 Oak Ave","city":"Springfield","state":"IL","zipCode":"62702",
           "latitude":"39.9","longitude":"-89.7"}
        ]}
        </script>
        <script type="application/ld+json">{"name":"Not a store","value":"x"}</script>
        </body></html>
        """;

    List<NearbyStore> stores = StoreLocatorParser.parse(html, StoreBrand.WALMART);

    assertEquals(2, stores.size());
    NearbyStore first = stores.get(0);
    assertEquals("123", first.getId());
    assertEquals("Walmart Supercenter #123", first.getName());
    assertEquals("1 Main St", first.getAddressLine());
    assertEquals("Springfield", first.getCity());
    assertEquals("IL", first.getState());
    assertEquals("62701", first.getPostalCode());
    assertEquals(Double.valueOf(39.8), first.getLatitude());
    assertEquals(Double.valueOf(-89.6), first.getLongitude());
    assertEquals("456", stores.get(1).getId());
  }

  @Test
  public void parsesEmbeddedJsonWithCoordinateAliases() {
    String html =
        """
        <html><body><script type="application/json">
        {"stores":[{"displayName":"Home Depot #642","storeId":"642",
          "addressLine1":"100 Market St","city":"San Francisco","state":"CA",
          "postalCode":"94103","lat":"37.77","lng":"-122.41"}]}
        </script></body></html>
        """;

    List<NearbyStore> stores = StoreLocatorParser.parse(html, StoreBrand.HOME_DEPOT);

    assertEquals(1, stores.size());
    NearbyStore store = stores.get(0);
    assertEquals("642", store.getId());
    assertEquals("Home Depot #642", store.getName());
    assertEquals("94103", store.getPostalCode());
    assertEquals(Double.valueOf(37.77), store.getLatitude());
    assertEquals(Double.valueOf(-122.41), store.getLongitude());
  }

  @Test
  public void malformedScriptsAreIgnored() {
    String html =
        "<html><body><script type=\"application/ld+json\">{not json</script></body></html>";

    assertTrue(StoreLocatorParser.parse(html, StoreBrand.TARGET).isEmpty());
    assertTrue(StoreLocatorParser.parse(null, StoreBrand.TARGET).isEmpty());
    assertTrue(StoreLocatorParser.parse("", StoreBrand.TARGET).isEmpty());
  }

  @Test
  public void invalidCoordinatesDoNotCreateStore() {
    String html =
        """
        <script type="application/ld+json">
        {"name":"Bad Store","address":{"postalCode":"00000"},
         "geo":{"latitude":999,"longitude":0}}
        </script>
        """;

    List<NearbyStore> stores = StoreLocatorParser.parse(html, StoreBrand.LOWES);

    assertTrue(stores.isEmpty());
  }
}
