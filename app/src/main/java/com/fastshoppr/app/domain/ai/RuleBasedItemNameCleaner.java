/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.ai;

import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.CleanItemRequest;
import com.fastshoppr.app.domain.model.CleanItemResult;
import com.fastshoppr.app.domain.text.TextNormalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Deterministic, offline item-name cleanup used whenever on-device AI is unavailable or disabled.
 */
@Singleton
public final class RuleBasedItemNameCleaner implements ItemNameCleaner {

  static final float CONFIDENCE = 0.8f;

  private static final Pattern LEADING_LIST_MARKER =
      Pattern.compile("^\\s*(?:[-*•·]|\\d{1,3}[.)])\\s+");
  private static final Pattern ABBREVIATION = Pattern.compile("(?i)\\b(tp|pb|gal)\\b");

  private static final Map<String, String> EXPANSIONS = createExpansions();

  private static Map<String, String> createExpansions() {
    Map<String, String> expansions = new LinkedHashMap<>();
    expansions.put("tp", "toilet paper");
    expansions.put("pb", "peanut butter");
    expansions.put("gal", "gallon");
    return Map.copyOf(expansions);
  }

  @Inject
  public RuleBasedItemNameCleaner() {}

  @Override
  public CompletableFuture<AiAvailability> availability() {
    return CompletableFuture.completedFuture(new AiAvailability.Available());
  }

  @Override
  public CompletableFuture<CleanItemResult> clean(CleanItemRequest request) {
    return CompletableFuture.completedFuture(cleanText(request.getRawText()));
  }

  /** Pure, deterministic cleanup of one item name. */
  public CleanItemResult cleanText(String rawText) {
    String collapsed = TextNormalizer.collapseWhitespace(rawText);
    if (collapsed == null || collapsed.isEmpty()) {
      return new CleanItemResult.NoChange();
    }
    String withoutMarker = LEADING_LIST_MARKER.matcher(collapsed).replaceFirst("");
    String expanded = expandAbbreviations(withoutMarker);
    String cleaned = capitalizeFirstLetter(TextNormalizer.collapseWhitespace(expanded));
    if (cleaned == null
        || cleaned.isBlank()
        || cleaned.length() > CleanItemRequest.MAX_TEXT_LENGTH) {
      return new CleanItemResult.NoChange();
    }
    if (cleaned.equals(collapsed.trim())) {
      return new CleanItemResult.NoChange();
    }
    return new CleanItemResult.Suggested(cleaned, CONFIDENCE, "Automatic rules cleanup");
  }

  private static String expandAbbreviations(String text) {
    Matcher matcher = ABBREVIATION.matcher(text);
    StringBuffer builder = new StringBuffer();
    while (matcher.find()) {
      String expansion = EXPANSIONS.get(matcher.group(1).toLowerCase(Locale.US));
      matcher.appendReplacement(builder, Matcher.quoteReplacement(expansion));
    }
    matcher.appendTail(builder);
    return builder.toString();
  }

  private static String capitalizeFirstLetter(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    char first = text.charAt(0);
    if (!Character.isLetter(first) || Character.isUpperCase(first)) {
      return text;
    }
    return Character.toUpperCase(first) + text.substring(1);
  }

  @Override
  public void close() {
    // Stateless.
  }
}
