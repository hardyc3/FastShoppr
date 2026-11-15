/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.homedepot;

import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.LookupSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/** Parses Home Depot product/availability content for aisle and bay. */
public final class HomeDepotAisleParser {

  private static final Pattern AISLE_BAY =
      Pattern.compile(
          "(?i)\\baisle\\s*[:#]?\\s*([A-Z0-9.-]+)(?:\\s*[,/\\-]?\\s*bay\\s*[:#]?\\s*([A-Z0-9.-]+))?");

  private HomeDepotAisleParser() {}

  /**
   * Calculates confidence from observable extraction evidence. Higher confidence when the aisle/bay
   * is found in visible page text (doc.text()) with product metadata. Lower confidence for regex
   * matches on raw HTML, which may appear in script tags or other non-visible contexts.
   */
  private static float calculateConfidence(boolean foundInVisibleText, boolean hasProductName) {
    // Visible text extraction with product metadata: highest confidence
    if (foundInVisibleText && hasProductName) {
      return 0.9f;
    }
    // Visible text extraction without product metadata: medium confidence
    if (foundInVisibleText) {
      return 0.75f;
    }
    // Raw HTML regex fallback: lowest confidence, marked for review when appropriate
    if (!hasProductName) {
      return 0.5f; // no product context -> review
    }
    // Raw HTML with product name: moderate confidence
    return 0.7f;
  }

  public static StoreLookupResult parseProductPage(
      String html, String productUrl, String fallbackProductName) {
    if (html == null || html.isBlank()) {
      return new StoreLookupResult.Error(
          new com.fastshoppr.app.domain.error.AppError.ParserFailed("Empty response"));
    }
    String lower = html.toLowerCase(Locale.US);
    if (lower.contains("access denied") || lower.contains("errors.edgesuite.net")) {
      // Akamai's block page says "Access Denied"; generic Akamai CDN references are not blocks
      return new StoreLookupResult.Blocked("Home Depot access blocked.");
    }

    Document doc = Jsoup.parse(html, productUrl == null ? "" : productUrl);
    String title = doc.title();
    if (title != null && title.toLowerCase(Locale.US).contains("access denied")) {
      return new StoreLookupResult.Blocked("Home Depot access blocked.");
    }

    // Prefer explicit in-store availability text patterns (visible page text).
    List<StoreLookupResult.Found> found = new ArrayList<>();
    String effectiveName = fallbackProductName;
    if (effectiveName == null || effectiveName.isBlank()) {
      effectiveName = firstNonBlank(meta(doc, "og:title"), title);
    }
    boolean hasProductName = effectiveName != null && !effectiveName.isBlank();

    // Path 1: scan visible text via Jsoup (most reliable)
    Matcher m = AISLE_BAY.matcher(doc.text());
    while (m.find()) {
      String aisle = clean(m.group(1));
      String bay = clean(m.group(2));
      if (aisle != null) {
        found.add(
            new StoreLookupResult.Found(
                aisle,
                bay,
                effectiveName,
                productUrl,
                calculateConfidence(/* foundInVisibleText= */ true, hasProductName),
                LookupSource.HOME_DEPOT_HTTP));
      }
    }
    // Path 2: scan raw HTML/JSON blobs as fallback (less reliable).
    if (found.isEmpty()) {
      Matcher m2 = AISLE_BAY.matcher(html);
      while (m2.find()) {
        String aisle = clean(m2.group(1));
        String bay = clean(m2.group(2));
        if (aisle != null) {
          found.add(
              new StoreLookupResult.Found(
                  aisle,
                  bay,
                  effectiveName,
                  productUrl,
                  calculateConfidence(/* foundInVisibleText= */ false, hasProductName),
                  LookupSource.HOME_DEPOT_HTTP));
        }
      }
    }

    if (found.isEmpty()) {
      if (lower.contains("not available") || lower.contains("unavailable at")) {
        return new StoreLookupResult.NotFound("No in-store aisle shown");
      }
      return new StoreLookupResult.NotFound("No in-store aisle shown");
    }
    // Deduplicate by aisle/bay/name
    List<StoreLookupResult.Found> unique = dedupe(found);
    if (unique.size() == 1) {
      return unique.get(0);
    }
    return new StoreLookupResult.Multiple(unique);
  }

  private static List<StoreLookupResult.Found> dedupe(List<StoreLookupResult.Found> found) {
    List<StoreLookupResult.Found> out = new ArrayList<>();
    for (StoreLookupResult.Found f : found) {
      boolean dup = false;
      for (StoreLookupResult.Found e : out) {
        if (eq(e.getAisle(), f.getAisle())
            && eq(e.getBay(), f.getBay())
            && eq(e.getProductName(), f.getProductName())) {
          dup = true;
          break;
        }
      }
      if (!dup) {
        out.add(f);
      }
    }
    return out;
  }

  private static boolean eq(String a, String b) {
    return a == null ? b == null : a.equalsIgnoreCase(b == null ? "" : b);
  }

  private static String clean(String v) {
    if (v == null) {
      return null;
    }
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private static String meta(Document doc, String property) {
    var el = doc.selectFirst("meta[property=" + property + "]");
    if (el == null) {
      return null;
    }
    return clean(el.attr("content"));
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String v : values) {
      if (v != null && !v.trim().isEmpty()) {
        return v.trim();
      }
    }
    return null;
  }
}
