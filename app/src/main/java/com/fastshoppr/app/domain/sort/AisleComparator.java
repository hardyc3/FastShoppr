/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.sort;

import com.fastshoppr.app.domain.model.SortDirection;
import com.fastshoppr.app.domain.text.TextNormalizer;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AisleComparator implements Comparator<String> {

  private static final Pattern FIRST_DIGITS = Pattern.compile("\\d+");

  private final SortDirection direction;

  public AisleComparator(SortDirection direction) {
    this.direction = Objects.requireNonNull(direction, "direction must not be null");
  }

  @Override
  public int compare(String left, String right) {
    boolean leftUnresolved = isUnresolved(left);
    boolean rightUnresolved = isUnresolved(right);
    if (leftUnresolved || rightUnresolved) {
      if (leftUnresolved && rightUnresolved) {
        return applyDirection(compareUnresolved(left, right));
      }
      return leftUnresolved ? 1 : -1;
    }
    return applyDirection(compareResolvedValues(left, right));
  }

  public SortDirection getDirection() {
    return direction;
  }

  public static boolean isUnresolved(String value) {
    if (value == null || value.trim().isEmpty()) {
      return true;
    }
    return !FIRST_DIGITS.matcher(value).find();
  }

  public static int compareResolvedValues(String left, String right) {
    Matcher leftMatcher = FIRST_DIGITS.matcher(left);
    Matcher rightMatcher = FIRST_DIGITS.matcher(right);
    if (!leftMatcher.find() || !rightMatcher.find()) {
      throw new IllegalArgumentException("both values must contain digits");
    }

    BigInteger leftNumber = new BigInteger(leftMatcher.group());
    BigInteger rightNumber = new BigInteger(rightMatcher.group());
    int numericComparison = leftNumber.compareTo(rightNumber);
    if (numericComparison != 0) {
      return numericComparison;
    }

    int prefixComparison =
        normalizePart(left.substring(0, leftMatcher.start()))
            .compareTo(normalizePart(right.substring(0, rightMatcher.start())));
    if (prefixComparison != 0) {
      return prefixComparison;
    }

    return normalizePart(left.substring(leftMatcher.end()))
        .compareTo(normalizePart(right.substring(rightMatcher.end())));
  }

  private static int compareUnresolved(String left, String right) {
    return normalizePart(left).compareTo(normalizePart(right));
  }

  private static String normalizePart(String value) {
    String trimmed = value == null ? "" : value.trim();
    return TextNormalizer.collapseWhitespace(trimmed).toLowerCase(Locale.US);
  }

  private int applyDirection(int comparison) {
    return direction == SortDirection.ASC ? comparison : -comparison;
  }
}
