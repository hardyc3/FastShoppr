/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.data.remote.lookup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class LookupQueryNormalizer {

  private static final Pattern LEADING_MARKER = Pattern.compile("^\\s*(?:[-*•]|\\d+[.)])\\s+");
  private static final Pattern QUANTITY_SIZE_TOKEN =
      Pattern.compile(
          "^(?:\\d+(?:\\.\\d+)?|one|two|three|four|five|six|pack|pk|ct|count|oz|ounce|ounces|lb|lbs|pound|pounds|gal|gallon|gallons|qt|quart|quarts|pt|pint|pints|ml|l|liter|liters|g|kg)$",
          Pattern.CASE_INSENSITIVE);

  private LookupQueryNormalizer() {}

  public static String normalize(String text) {
    if (text == null) {
      return "";
    }
    String value = LEADING_MARKER.matcher(text).replaceFirst("");
    value = value.toLowerCase(Locale.US);
    value = value.replaceAll("[^a-z0-9\\s]", " ");
    value = value.replaceAll("\\s+", " ").trim();
    return value;
  }

  /** Returns up to 3 alternate query variants in priority order, without duplicates. */
  public static List<String> alternates(String displayName) {
    Set<String> out = new LinkedHashSet<>();
    String normalized = normalize(displayName);
    if (!normalized.isEmpty()) {
      out.add(normalized);
    }
    String withoutQty = removeQuantityAndSizeTokens(normalized);
    if (!withoutQty.isEmpty()) {
      out.add(withoutQty);
    }
    String singular = singularizeLastWord(withoutQty.isEmpty() ? normalized : withoutQty);
    if (!singular.isEmpty()) {
      out.add(singular);
    }
    String firstTwo = firstTwoMeaningfulWords(withoutQty.isEmpty() ? normalized : withoutQty);
    if (!firstTwo.isEmpty()) {
      out.add(firstTwo);
    }
    List<String> list = new ArrayList<>(out);
    if (list.size() > 3) {
      return list.subList(0, 3);
    }
    return list;
  }

  private static String removeQuantityAndSizeTokens(String normalized) {
    if (normalized == null || normalized.isEmpty()) {
      return "";
    }
    String[] parts = normalized.split("\\s+");
    StringBuilder b = new StringBuilder();
    for (String p : parts) {
      if (QUANTITY_SIZE_TOKEN.matcher(p).matches()) {
        continue;
      }
      if (b.length() > 0) {
        b.append(' ');
      }
      b.append(p);
    }
    return b.toString().trim();
  }

  private static String singularizeLastWord(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    String[] parts = text.split("\\s+");
    if (parts.length == 0) {
      return "";
    }
    String last = parts[parts.length - 1];
    String singular = simpleSingular(last);
    if (singular.equals(last)) {
      return "";
    }
    parts[parts.length - 1] = singular;
    return String.join(" ", parts).trim();
  }

  private static String simpleSingular(String word) {
    if (word.length() <= 3) {
      return word;
    }
    if (word.endsWith("ies") && word.length() > 4) {
      return word.substring(0, word.length() - 3) + "y";
    }
    if (word.endsWith("ses") || word.endsWith("xes") || word.endsWith("zes")) {
      return word.substring(0, word.length() - 2);
    }
    if (word.endsWith("s") && !word.endsWith("ss")) {
      return word.substring(0, word.length() - 1);
    }
    return word;
  }

  private static String firstTwoMeaningfulWords(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    String[] parts = text.split("\\s+");
    List<String> meaningful = new ArrayList<>();
    for (String p : parts) {
      if (p.length() < 2) {
        continue;
      }
      meaningful.add(p);
      if (meaningful.size() == 2) {
        break;
      }
    }
    if (meaningful.isEmpty()) {
      return "";
    }
    return String.join(" ", meaningful);
  }
}
