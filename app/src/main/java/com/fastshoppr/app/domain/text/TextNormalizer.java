/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.text;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TextNormalizer {

  private static final Pattern LEADING_LIST_MARKER = Pattern.compile("^\\s*(?:[-*]|\\d+[.)])\\s*");
  private static final Pattern NON_LOOKUP_CHARACTERS = Pattern.compile("[^a-z0-9\\s]");
  private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");

  private TextNormalizer() {}

  public static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static String collapseWhitespace(String value) {
    if (value == null) {
      return null;
    }
    return REPEATED_WHITESPACE.matcher(value.trim()).replaceAll(" ");
  }

  public static String normalizeForLookup(String displayName) {
    if (displayName == null) {
      return "";
    }
    String normalized = displayName.toLowerCase(Locale.US);
    normalized = LEADING_LIST_MARKER.matcher(normalized).replaceFirst("");
    normalized = NON_LOOKUP_CHARACTERS.matcher(normalized).replaceAll(" ");
    return collapseWhitespace(normalized);
  }
}
