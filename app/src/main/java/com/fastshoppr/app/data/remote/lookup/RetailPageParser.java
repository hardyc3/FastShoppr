/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import com.fastshoppr.app.domain.error.AppError;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.StoreBrand;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Parses public retail search pages and the first product page they link to. */
public final class RetailPageParser {

  private static final Pattern AISLE_BAY =
      Pattern.compile(
          "(?i)\\baisle\\s*[:#]?\\s*([A-Z0-9.-]+)(?:\\s*[,/\\-]?\\s*bay\\s*[:#]?\\s*([A-Z0-9.-]+))?");
  private static final Pattern STORE_AVAILABILITY_AISLE =
      Pattern.compile(
          "(?i)(?:available at|availability at|in-store availability(?: at)?)"
              + "\\s+([^:]{1,80}):\\s*aisle\\s*[:#]?\\s*([A-Z0-9.-]+)"
              + "(?:\\s*[,/\\-]?\\s*bay\\s*[:#]?\\s*([A-Z0-9.-]+))?");
  private static final List<String> BLOCK_MARKERS =
      List.of(
          "access denied",
          "errors.edgesuite.net",
          "human verification",
          "robot or human",
          "verify you are human",
          "request blocked",
          "oops!! something went wrong");

  private RetailPageParser() {}

  /** True when a search or product page is an access block or bot challenge. */
  public static boolean isBlockedPage(String html) {
    if (html == null || html.isBlank()) {
      return false;
    }
    String lower = html.toLowerCase(Locale.US);
    return BLOCK_MARKERS.stream().anyMatch(lower::contains);
  }

  /** Returns the first same-retailer product link in document order, or null. */
  public static String firstProductUrl(
      String html, String baseUrl, String host, String productPathToken) {
    List<Candidate> candidates = productCandidates(html, baseUrl, host, productPathToken);
    return candidates.isEmpty() ? null : candidates.get(0).url;
  }

  /**
   * Returns the first product link whose text overlaps the user's query, or null.
   *
   * <p>Retail search pages put unrelated carousel products first, so document order alone is not
   * enough. Ties keep document order. No overlap means no safe match.
   */
  public static String firstProductUrl(
      String html, String baseUrl, String host, String productPathToken, String query) {
    List<Candidate> candidates = productCandidates(html, baseUrl, host, productPathToken);
    Set<String> queryTokens = tokens(query);
    if (queryTokens.isEmpty()) {
      return candidates.isEmpty() ? null : candidates.get(0).url;
    }
    for (Candidate candidate : candidates) {
      // First matching link wins; a later link with more overlap must not replace it (D-055).
      if (overlap(queryTokens, tokens(candidate.text)) > 0) {
        return candidate.url;
      }
    }
    return null;
  }

  private static List<Candidate> productCandidates(
      String html, String baseUrl, String host, String productPathToken) {
    List<Candidate> out = new ArrayList<>();
    if (html == null || html.isBlank() || host == null || productPathToken == null) {
      return out;
    }
    Document doc = Jsoup.parse(html, baseUrl == null ? "" : baseUrl);
    for (Element link : doc.select("a[href]")) {
      String url = link.absUrl("href");
      if (!isProductUrl(url, host, productPathToken)) {
        continue;
      }
      String text = firstNonBlank(link.text(), link.attr("title"), link.attr("aria-label"), "");
      out.add(new Candidate(url, text));
    }
    return out;
  }

  private static Set<String> tokens(String value) {
    Set<String> out = new java.util.LinkedHashSet<>();
    if (value == null) {
      return out;
    }
    String[] parts = value.toLowerCase(Locale.US).split("[^a-z0-9]+");
    for (String part : parts) {
      if (part.length() > 1) {
        out.add(canonical(part));
      }
    }
    return out;
  }

  private static String canonical(String token) {
    // "towels" and "towel" should match; both sides use the same canonical form.
    return token.length() > 3 && token.endsWith("s") && !token.endsWith("ss")
        ? token.substring(0, token.length() - 1)
        : token;
  }

  private static int overlap(Set<String> queryTokens, Set<String> candidateTokens) {
    int score = 0;
    for (String token : queryTokens) {
      if (candidateTokens.contains(token)) {
        score++;
      }
    }
    return score;
  }

  private static final class Candidate {
    private final String url;
    private final String text;

    private Candidate(String url, String text) {
      this.url = url;
      this.text = text;
    }
  }

  /** Extracts aisle/bay from a public product page and tags the result with its lookup source. */
  public static StoreLookupResult parseProductPage(
      String html,
      String productUrl,
      String fallbackProductName,
      StoreBrand brand,
      LookupSource source) {
    return parseProductPage(html, productUrl, fallbackProductName, brand, source, null, null);
  }

  /**
   * Extracts a visible aisle/bay for the selected store.
   *
   * <p>A store-specific availability line wins when its label matches the selected store name or
   * city. A store-specific line for a different store is rejected rather than inferred. Generic
   * aisle text with no store label is returned at low confidence so the repository marks it {@code
   * NEEDS_REVIEW}.
   */
  public static StoreLookupResult parseProductPage(
      String html,
      String productUrl,
      String fallbackProductName,
      StoreBrand brand,
      LookupSource source,
      String storeName,
      String storeCity) {
    if (html == null || html.isBlank()) {
      return new StoreLookupResult.Error(new AppError.ParserFailed("Empty response"));
    }
    if (isBlockedPage(html)) {
      String name = brand == null ? "Store" : brand.getDisplayName();
      return new StoreLookupResult.Blocked(name + " access blocked.");
    }

    Document doc = Jsoup.parse(html, productUrl == null ? "" : productUrl);
    String title = doc.title();
    if (title != null
        && BLOCK_MARKERS.stream().anyMatch(m -> title.toLowerCase(Locale.US).contains(m))) {
      String name = brand == null ? "Store" : brand.getDisplayName();
      return new StoreLookupResult.Blocked(name + " access blocked.");
    }

    String pageProductName = firstNonBlank(meta(doc, "og:title"), title);
    String productName = firstNonBlank(pageProductName, fallbackProductName);
    boolean hasPageProductName = pageProductName != null && !pageProductName.isBlank();
    List<StoreLookupResult.Found> found =
        collect(
            doc.text(), productUrl, productName, source, hasPageProductName, storeName, storeCity);
    if (found.isEmpty()) {
      return new StoreLookupResult.NotFound("No in-store aisle shown");
    }
    List<StoreLookupResult.Found> unique = dedupe(found);
    if (unique.size() == 1) {
      return unique.get(0);
    }
    return new StoreLookupResult.Multiple(unique);
  }

  private static boolean isProductUrl(String url, String host, String productPathToken) {
    if (url == null || url.isBlank()) {
      return false;
    }
    try {
      URI uri = URI.create(url);
      if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
        return false;
      }
      String actualHost = uri.getHost().toLowerCase(Locale.US);
      String allowed = host.toLowerCase(Locale.US);
      boolean sameHost = actualHost.equals(allowed) || actualHost.endsWith("." + allowed);
      return sameHost && uri.getPath() != null && uri.getPath().contains(productPathToken);
    } catch (IllegalArgumentException malformed) {
      return false;
    }
  }

  private static List<StoreLookupResult.Found> collect(
      String text,
      String productUrl,
      String productName,
      LookupSource source,
      boolean hasPageProductName,
      String storeName,
      String storeCity) {
    List<StoreLookupResult.Found> out = new ArrayList<>();
    Set<String> contextTokens = tokens(storeName);
    contextTokens.addAll(tokens(storeCity));
    Matcher storeMatcher = STORE_AVAILABILITY_AISLE.matcher(text);
    boolean sawStoreLine = false;
    while (storeMatcher.find()) {
      sawStoreLine = true;
      if (!contextTokens.isEmpty() && overlap(contextTokens, tokens(storeMatcher.group(1))) == 0) {
        continue;
      }
      String aisle = clean(storeMatcher.group(2));
      if (aisle == null) {
        continue;
      }
      out.add(
          found(
              aisle,
              clean(storeMatcher.group(3)),
              productName,
              productUrl,
              hasPageProductName ? 0.9f : 0.8f,
              source));
    }
    // A labelled aisle belongs to a specific store. Do not fall back to another store's line.
    if (sawStoreLine) {
      return out;
    }

    float confidence = hasPageProductName ? 0.7f : 0.55f;
    Matcher matcher = AISLE_BAY.matcher(text);
    while (matcher.find()) {
      String aisle = clean(matcher.group(1));
      if (aisle != null) {
        out.add(found(aisle, clean(matcher.group(2)), productName, productUrl, confidence, source));
      }
    }
    return out;
  }

  private static StoreLookupResult.Found found(
      String aisle,
      String bay,
      String productName,
      String productUrl,
      float confidence,
      LookupSource source) {
    return new StoreLookupResult.Found(aisle, bay, productName, productUrl, confidence, source);
  }

  private static List<StoreLookupResult.Found> dedupe(List<StoreLookupResult.Found> found) {
    List<StoreLookupResult.Found> out = new ArrayList<>();
    for (StoreLookupResult.Found candidate : found) {
      boolean duplicate = false;
      for (StoreLookupResult.Found existing : out) {
        if (eq(existing.getAisle(), candidate.getAisle())
            && eq(existing.getBay(), candidate.getBay())
            && eq(existing.getProductName(), candidate.getProductName())) {
          duplicate = true;
          break;
        }
      }
      if (!duplicate) {
        out.add(candidate);
      }
    }
    return out;
  }

  private static boolean eq(String a, String b) {
    return a == null ? b == null : a.equalsIgnoreCase(b == null ? "" : b);
  }

  private static String clean(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    for (int i = 0; i < trimmed.length(); i++) {
      if (Character.isLetterOrDigit(trimmed.charAt(i))) {
        return trimmed;
      }
    }
    // Punctuation-only matches such as "aisle." are not aisle values.
    return null;
  }

  private static String meta(Document doc, String property) {
    Element element = doc.selectFirst("meta[property=" + property + "]");
    return element == null ? null : clean(element.attr("content"));
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) {
        return value.trim();
      }
    }
    return null;
  }
}
