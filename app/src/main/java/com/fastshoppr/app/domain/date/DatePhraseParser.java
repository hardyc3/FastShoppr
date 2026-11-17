/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.domain.date;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic date-phrase parsing for deferral. No Android dependencies. */
public final class DatePhraseParser {

  public enum Anchor {
    TODAY,
    CURRENT_LIST_DATE,
    SCHEDULE
  }

  public record ParsedDate(
      String phrase,
      String itemText,
      Optional<LocalDate> resolvedDate,
      Anchor anchor,
      boolean needsConfirmation,
      boolean requiresSchedule,
      String note) {}

  private record Rule(
      Pattern pattern,
      Anchor anchor,
      boolean needsConfirmation,
      boolean requiresSchedule,
      String note,
      DateResolver resolver) {}

  @FunctionalInterface
  private interface DateResolver {
    LocalDate resolve(Matcher matcher, LocalDate today, LocalDate currentListDate);
  }

  private static final int MAX_DAYS = 365;
  private static final int MAX_WEEKS = 52;
  private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[\\s.,!?;:]+$");

  private static final Rule TOMORROW =
      rule("\\btomorrow\\b", Anchor.TODAY, false, false, "", (m, today, list) -> today.plusDays(1));

  private static final Rule TODAY =
      rule("\\btoday\\b", Anchor.TODAY, false, false, "", (m, today, list) -> today);

  private static final Rule IN_N_DAYS =
      rule(
          "\\bin\\s+(\\d{1,3})\\s+days?\\b",
          Anchor.CURRENT_LIST_DATE,
          false,
          false,
          "",
          (m, today, list) -> list.plusDays(clamp(parseInt(m.group(1)), MAX_DAYS)));

  private static final Rule IN_N_WEEKS =
      rule(
          "\\bin\\s+(\\d{1,2})\\s+weeks?\\b",
          Anchor.CURRENT_LIST_DATE,
          false,
          false,
          "",
          (m, today, list) -> list.plusDays(7L * clamp(parseInt(m.group(1)), MAX_WEEKS)));

  private static final Rule IN_A_WEEK =
      rule(
          "\\bin\\s+a\\s+week\\b",
          Anchor.CURRENT_LIST_DATE,
          false,
          false,
          "",
          (m, today, list) -> list.plusWeeks(1));

  private static final Rule NEXT_WEEK =
      rule(
          "\\bnext\\s+week\\b",
          Anchor.CURRENT_LIST_DATE,
          false,
          false,
          "",
          (m, today, list) -> list.plusWeeks(1));

  private static final Rule WAIT_A_WEEK =
      rule(
          "\\bwait\\s+a\\s+week\\b",
          Anchor.CURRENT_LIST_DATE,
          false,
          false,
          "",
          (m, today, list) -> list.plusWeeks(1));

  private static final Rule NEXT_MONTH =
      rule(
          "\\bnext\\s+month\\b",
          Anchor.CURRENT_LIST_DATE,
          false,
          false,
          "",
          (m, today, list) -> list.plusMonths(1));

  private static final Rule THIS_WEEKEND =
      rule(
          "\\bthis\\s+weekend\\b",
          Anchor.CURRENT_LIST_DATE,
          false,
          false,
          "",
          (m, today, list) -> list.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)));

  private static final Rule NEXT_SHOPPING_DAY =
      rule(
          "\\bnext\\s+shopping\\s+day\\b",
          Anchor.SCHEDULE,
          false,
          true,
          "Requires the recurring schedule to resolve.",
          (m, today, list) -> null);

  private static final Rule[] RULES = {
    NEXT_SHOPPING_DAY,
    IN_N_WEEKS,
    IN_N_DAYS,
    IN_A_WEEK,
    WAIT_A_WEEK,
    NEXT_WEEK,
    NEXT_MONTH,
    THIS_WEEKEND,
    TOMORROW,
    TODAY
  };

  private DatePhraseParser() {}

  public static Optional<ParsedDate> parse(
      String text, LocalDate currentListDate, LocalDate today) {
    if (text == null || text.isBlank() || currentListDate == null || today == null) {
      return Optional.empty();
    }

    String trimmed = text.trim();
    String withoutTrailingPunctuation = TRAILING_PUNCTUATION.matcher(trimmed).replaceFirst("");
    String lower = withoutTrailingPunctuation.toLowerCase(Locale.US);

    for (Rule rule : RULES) {
      Matcher matcher = rule.pattern().matcher(lower);
      if (!matcher.find() || matcher.end() != lower.length()) {
        continue;
      }

      String itemText = withoutTrailingPunctuation.substring(0, matcher.start()).trim();
      if (itemText.isEmpty()) {
        return Optional.empty();
      }

      LocalDate resolved = rule.resolver().resolve(matcher, today, currentListDate);
      boolean past = resolved != null && resolved.isBefore(today);
      boolean needsConfirmation = rule.needsConfirmation() || past;
      String note = rule.note() == null ? "" : rule.note();
      if (past) {
        note = appendNote(note, "Resolved date is before today.");
      }

      return Optional.of(
          new ParsedDate(
              withoutTrailingPunctuation.substring(matcher.start(), matcher.end()),
              itemText,
              Optional.ofNullable(resolved),
              rule.anchor(),
              needsConfirmation,
              rule.requiresSchedule(),
              note));
    }

    return Optional.empty();
  }

  private static Rule rule(
      String regex,
      Anchor anchor,
      boolean needsConfirmation,
      boolean requiresSchedule,
      String note,
      DateResolver resolver) {
    return new Rule(
        Pattern.compile(regex, Pattern.CASE_INSENSITIVE),
        anchor,
        needsConfirmation,
        requiresSchedule,
        note,
        resolver);
  }

  private static int parseInt(String value) {
    return Integer.parseInt(value);
  }

  private static int clamp(int value, int max) {
    return Math.max(1, Math.min(value, max));
  }

  private static String appendNote(String existing, String extra) {
    return existing == null || existing.isBlank() ? extra : existing + " " + extra;
  }
}
