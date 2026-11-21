/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.storelocator;

import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.NearbyStoreSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Extracts branch candidates from a retailer store-locator page.
 *
 * <p>The parser prefers schema.org LocalBusiness JSON-LD and also accepts JSON embedded in {@code
 * application/json} script tags. Unknown shapes yield an empty list; the caller then asks the user
 * to enter the store manually. No retailer-specific CSS selectors are used.
 */
public final class StoreLocatorParser {

  private static final int MAX_DEPTH = 8;

  private StoreLocatorParser() {}

  public static List<NearbyStore> parse(String html, StoreBrand brand) {
    if (html == null || html.isBlank()) {
      return List.of();
    }
    Document document = Jsoup.parse(html);
    List<JsonObject> placeObjects = new ArrayList<>();
    for (Element script :
        document.select("script[type=application/ld+json], script[type=application/json]")) {
      String data = script.data();
      if (data == null || data.isBlank()) {
        continue;
      }
      try {
        collectPlaces(JsonParser.parseString(data), 0, placeObjects);
      } catch (JsonParseException malformedScript) {
        // One malformed script must not discard the rest of the page.
      }
    }

    Map<String, NearbyStore> results = new LinkedHashMap<>();
    for (JsonObject object : placeObjects) {
      NearbyStore store = toNearbyStore(object, brand);
      if (store != null) {
        results.putIfAbsent(dedupeKey(store), store);
      }
    }
    return List.copyOf(results.values());
  }

  private static void collectPlaces(JsonElement element, int depth, List<JsonObject> output) {
    if (element == null || element.isJsonNull() || depth > MAX_DEPTH) {
      return;
    }
    if (element.isJsonArray()) {
      for (JsonElement child : element.getAsJsonArray()) {
        collectPlaces(child, depth + 1, output);
      }
      return;
    }
    if (!element.isJsonObject()) {
      return;
    }
    JsonObject object = element.getAsJsonObject();
    if (looksLikePlace(object)) {
      output.add(object);
    }
    for (Map.Entry<String, JsonElement> member : object.entrySet()) {
      String key = member.getKey();
      if ("address".equals(key) || "geo".equals(key)) {
        continue;
      }
      JsonElement value = member.getValue();
      if (value != null && (value.isJsonObject() || value.isJsonArray())) {
        collectPlaces(value, depth + 1, output);
      }
    }
  }

  private static boolean looksLikePlace(JsonObject object) {
    boolean hasName =
        hasText(object, "name")
            || hasText(object, "displayName")
            || hasText(object, "storeName")
            || hasText(object, "locationName");
    boolean hasAddress =
        hasNonBlankAddress(object.get("address"))
            || hasText(object, "addressLine1")
            || hasText(object, "address1")
            || hasText(object, "city")
            || hasText(object, "postalCode")
            || hasText(object, "zipCode");
    boolean hasCoordinates =
        object.has("geo")
            || object.has("latitude")
            || object.has("lat")
            || object.has("longitude")
            || object.has("lng");
    return hasName && (hasAddress || hasCoordinates);
  }

  private static NearbyStore toNearbyStore(JsonObject object, StoreBrand brand) {
    String name =
        firstText(object, "name", "displayName", "storeName", "locationName", "storeDisplayName");
    if (name == null || name.isBlank()) {
      return null;
    }
    String id =
        firstText(object, "storeNumber", "storeId", "storeCode", "branchCode", "identifier", "id");
    if (id == null || id.isBlank()) {
      id = name;
    }

    String addressLine = firstText(object, "addressLine1", "address1", "streetAddress");
    String city = firstText(object, "city", "addressLocality");
    String state = firstText(object, "state", "addressRegion");
    String postalCode = firstText(object, "postalCode", "zipCode", "zip");
    JsonElement address = object.get("address");
    if (address != null && address.isJsonObject()) {
      JsonObject addressObject = address.getAsJsonObject();
      if (addressLine == null) {
        addressLine = firstText(addressObject, "streetAddress", "addressLine1", "address1");
      }
      if (city == null) {
        city = firstText(addressObject, "addressLocality", "city");
      }
      if (state == null) {
        state = firstText(addressObject, "addressRegion", "state");
      }
      if (postalCode == null) {
        postalCode = firstText(addressObject, "postalCode", "zipCode", "zip");
      }
    } else if (addressLine == null && address != null && address.isJsonPrimitive()) {
      addressLine = address.getAsString();
    }

    Double latitude = firstNumber(object, "latitude", "lat");
    Double longitude = firstNumber(object, "longitude", "lng", "lon");
    JsonElement geo = object.get("geo");
    if (geo != null && geo.isJsonObject()) {
      if (latitude == null) {
        latitude = firstNumber(geo.getAsJsonObject(), "latitude", "lat");
      }
      if (longitude == null) {
        longitude = firstNumber(geo.getAsJsonObject(), "longitude", "lng", "lon");
      }
    }

    try {
      return new NearbyStore(
          id,
          brand,
          name,
          addressLine,
          city,
          state,
          postalCode,
          latitude,
          longitude,
          NearbyStoreSource.LOCATOR);
    } catch (IllegalArgumentException invalidCandidate) {
      return null;
    }
  }

  private static String dedupeKey(NearbyStore store) {
    return store.getBrand()
        + "|"
        + store.getId()
        + "|"
        + store.getName()
        + "|"
        + store.getPostalCode();
  }

  private static boolean hasNonBlankAddress(JsonElement address) {
    if (address == null || address.isJsonNull()) {
      return false;
    }
    if (address.isJsonPrimitive()) {
      String value = address.getAsString();
      return value != null && !value.isBlank();
    }
    if (!address.isJsonObject()) {
      return false;
    }
    JsonObject object = address.getAsJsonObject();
    return hasText(object, "streetAddress", "addressLine1", "address1")
        || hasText(object, "addressLocality", "city")
        || hasText(object, "addressRegion", "state")
        || hasText(object, "postalCode", "zipCode", "zip");
  }

  private static boolean hasText(JsonObject object, String... keys) {
    for (String key : keys) {
      String value = firstText(object, key);
      if (value != null && !value.isBlank()) {
        return true;
      }
    }
    return false;
  }

  private static String firstText(JsonObject object, String... keys) {
    for (String key : keys) {
      JsonElement value = object.get(key);
      if (value != null && value.isJsonPrimitive()) {
        return value.getAsString();
      }
    }
    return null;
  }

  private static Double firstNumber(JsonObject object, String... keys) {
    for (String key : keys) {
      JsonElement value = object.get(key);
      if (value != null && value.isJsonPrimitive()) {
        try {
          return Double.valueOf(value.getAsString());
        } catch (NumberFormatException notANumber) {
          // Try the next alias.
        }
      }
    }
    return null;
  }
}
