/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.walmart;

import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.LookupSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts Walmart aisle/location candidates from rendered page content or __NEXT_DATA__ JSON. */
public final class WalmartAisleExtractor {

  private static final Pattern NEXT_DATA =
      Pattern.compile(
          "<script[^>]*id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern AISLE_TOKEN =
      Pattern.compile("(?i)\\b(?:aisle|location)\\s*[:#]?\\s*([A-Z0-9-]{1,10})\\b");

  private WalmartAisleExtractor() {}

  public static StoreLookupResult extractFromPage(String htmlOrJson) {
    if (htmlOrJson == null || htmlOrJson.isBlank()) {
      return new StoreLookupResult.Error(
          new com.fastshoppr.app.domain.error.AppError.ParserFailed("Empty response"));
    }
    String lower = htmlOrJson.toLowerCase(Locale.US);
    if (isBlockedPage(lower)) {
      return new StoreLookupResult.Blocked(
          "Walmart challenge or sign-in page. Open the Walmart store page, complete verification, then retry.");
    }

    List<String> jsonBlobs = new ArrayList<>();
    Matcher m = NEXT_DATA.matcher(htmlOrJson);
    while (m.find()) {
      jsonBlobs.add(m.group(1));
    }
    // also try raw if looks like json
    String trimmed = htmlOrJson.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      jsonBlobs.add(trimmed);
    }

    List<StoreLookupResult.Found> found = new ArrayList<>();
    for (String json : jsonBlobs) {
      try {
        JsonElement root = JsonParser.parseString(json);
        collectFromJson(root, found);
      } catch (Exception ignored) {
        // try next blob
      }
    }
    // fallback: regex scan text for aisle mentions
    if (found.isEmpty()) {
      Matcher am = AISLE_TOKEN.matcher(stripTags(htmlOrJson));
      while (am.find()) {
        String aisle = am.group(1);
        if (aisle != null && !aisle.isBlank()) {
          found.add(
              new StoreLookupResult.Found(
                  aisle.trim(), null, null, null, 0.5f, LookupSource.WALMART_WEBVIEW));
        }
      }
    }

    List<StoreLookupResult.Found> unique = dedupe(found);
    if (unique.isEmpty()) {
      if (lower.contains("out of stock") || lower.contains("not sold at")) {
        return new StoreLookupResult.NotFound("Item not stocked locally.");
      }
      return new StoreLookupResult.NotFound("No aisle/location found.");
    }
    if (unique.size() == 1) {
      return unique.get(0);
    }
    // Multiple candidates -> NEEDS_REVIEW per spec (Multiple result)
    return new StoreLookupResult.Multiple(unique);
  }

  /** Challenge-page markers that are specific enough not to match Walmart's normal page chrome. */
  private static final String[] BLOCKED_PAGE_MARKERS = {
    "robot or human",
    "id=\"px-captcha\"",
    "id='px-captcha'",
    "verify you are human",
    "unusual traffic",
    "please verify",
    "access denied",
    "sign in to continue",
    "sign-in required",
    "please sign in",
    "too many requests"
  };

  public static boolean isBlockedPage(String lowerHtml) {
    if (lowerHtml == null) {
      return false;
    }
    // Walmart ships PerimeterX scripts and CSP allowlist entries (*.perimeterx.net, *.px-cdn.net)
    // on normal pages, so the vendor name is not a challenge signal. Match the challenge UI and
    // block markers only.
    for (String marker : BLOCKED_PAGE_MARKERS) {
      if (lowerHtml.contains(marker)) {
        return true;
      }
    }
    return false;
  }

  private static void collectFromJson(JsonElement element, List<StoreLookupResult.Found> out) {
    if (element == null || element.isJsonNull()) {
      return;
    }
    if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();
      // Common patterns: location.displayValue / storeLocation / aisle info on items
      String aisle = firstString(obj, "aisle", "aisleId", "Aisle");
      String bay = firstString(obj, "bay", "Bay");
      String locDisplay = firstString(obj, "LocationDisplayValue", "locationDisplayValue");
      if (locDisplay != null && aisle == null) {
        aisle = locDisplay;
      }
      JsonElement locArr = obj.get("Location");
      if (locArr == null) {
        locArr = obj.get("location");
      }
      boolean handledLocation = false;
      if (locArr != null && locArr.isJsonArray()) {
        handledLocation = true;
        for (JsonElement le : locArr.getAsJsonArray()) {
          if (!le.isJsonObject()) continue;
          JsonObject lo = le.getAsJsonObject();
          String dv = firstString(lo, "displayValue", "LocationDisplayValue");
          String a = firstString(lo, "aisle", "zoneAisle");
          if (a == null && lo.has("aisle") && lo.get("aisle").isJsonObject()) {
            JsonObject ao = lo.getAsJsonObject("aisle");
            String zone = firstString(ao, "zone");
            String num = firstString(ao, "aisle");
            if (zone != null && num != null) {
              a = zone + num;
            } else if (num != null) {
              a = num;
            }
          }
          String chosen = dv != null ? dv : a;
          if (chosen != null) {
            String name = firstString(obj, "name", "productName", "title");
            String url = firstString(obj, "canonicalUrl", "productPageUrl", "url");
            out.add(
                new StoreLookupResult.Found(
                    chosen.trim(), bay, name, url, 0.9f, LookupSource.WALMART_WEBVIEW));
          }
        }
      } else if (aisle != null || locDisplay != null) {
        String chosen = locDisplay != null ? locDisplay : aisle;
        String name = firstString(obj, "name", "productName", "title");
        String url = firstString(obj, "canonicalUrl", "productPageUrl", "url");
        out.add(
            new StoreLookupResult.Found(
                chosen.trim(), bay, name, url, 0.85f, LookupSource.WALMART_WEBVIEW));
      }

      for (String key : obj.keySet()) {
        if (handledLocation && ("Location".equals(key) || "location".equals(key))) {
          continue;
        }
        collectFromJson(obj.get(key), out);
      }
      return;
    }
    if (element.isJsonArray()) {
      JsonArray arr = element.getAsJsonArray();
      for (JsonElement e : arr) {
        collectFromJson(e, out);
      }
    }
  }

  private static String firstString(JsonObject obj, String... keys) {
    for (String k : keys) {
      if (obj.has(k) && obj.get(k).isJsonPrimitive()) {
        try {
          String v = obj.get(k).getAsString();
          if (v != null && !v.trim().isEmpty()) {
            return v.trim();
          }
        } catch (Exception ignored) {
        }
      }
    }
    return null;
  }

  private static List<StoreLookupResult.Found> dedupe(List<StoreLookupResult.Found> found) {
    // Dedupe primarily by location; prefer entries with richer product metadata.
    java.util.Map<String, StoreLookupResult.Found> byLocation = new java.util.LinkedHashMap<>();
    for (StoreLookupResult.Found f : found) {
      String locKey = (safe(f.getAisle()) + "|" + safe(f.getBay())).toLowerCase(Locale.US);
      StoreLookupResult.Found existing = byLocation.get(locKey);
      if (existing == null || richness(f) > richness(existing)) {
        byLocation.put(locKey, f);
      }
    }
    List<StoreLookupResult.Found> out = new ArrayList<>(byLocation.values());
    // cap to avoid noisy multiples from deep JSON walks
    if (out.size() > 5) {
      return out.subList(0, 5);
    }
    return out;
  }

  private static int richness(StoreLookupResult.Found f) {
    int score = 0;
    if (f.getProductName() != null && !f.getProductName().isBlank()) score += 2;
    if (f.getProductUrl() != null && !f.getProductUrl().isBlank()) score += 1;
    if (f.getConfidence() != null) score += 1;
    return score;
  }

  private static String safe(String v) {
    return v == null ? "" : v.trim();
  }

  private static String stripTags(String html) {
    return html.replaceAll("<[^>]+>", " ");
  }
}
