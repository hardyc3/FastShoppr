package com.fastshoppr.spike.dates;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Dependency-free runner for the Phase 0 date-parser prototype.
 *
 * <p>Run with: docs/spikes/date-parser/run.sh
 */
public final class DatePhraseParserTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    private static final LocalDate LIST_DATE = LocalDate.of(2026, 11, 5);

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        parsesTomorrow();
        parsesToday();
        parsesInNDays();
        parsesNextWeek();
        parsesWaitAWeek();
        parsesInNWeeks();
        parsesNextMonth();
        parsesThisWeekend();
        parsesNextShoppingDay();
        ignoresNonDateText();
        requiresItemTextBeforePhrase();
        handlesTrailingPunctuation();
        flagsPastResolvedDate();
        handlesMonthEndAndLeapYear();
        handlesWeekendOnSaturday();
        resolvesNearestList();
        createsListWhenNoneMatches();
        flagsPastTargetDate();

        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void parsesTomorrow() {
        expect(
                "tomorrow",
                "batteries tomorrow",
                LIST_DATE,
                TODAY.plusDays(1),
                "batteries",
                DatePhraseParser.Anchor.TODAY,
                false,
                false);
    }

    private static void parsesToday() {
        expect(
                "today",
                "milk today",
                LIST_DATE,
                TODAY,
                "milk",
                DatePhraseParser.Anchor.TODAY,
                false,
                false);
    }

    private static void parsesInNDays() {
        expect(
                "in 3 days",
                "paper towels in 3 days",
                LIST_DATE,
                TODAY.plusDays(3),
                "paper towels",
                DatePhraseParser.Anchor.TODAY,
                true,
                false);
    }

    private static void parsesNextWeek() {
        expect(
                "next week",
                "milk next week",
                LIST_DATE,
                LIST_DATE.plusWeeks(1),
                "milk",
                DatePhraseParser.Anchor.CURRENT_LIST_DATE,
                false,
                false);
    }

    private static void parsesWaitAWeek() {
        expect(
                "wait a week",
                "toilet paper wait a week",
                LIST_DATE,
                LIST_DATE.plusWeeks(1),
                "toilet paper",
                DatePhraseParser.Anchor.CURRENT_LIST_DATE,
                false,
                false);
    }

    private static void parsesInNWeeks() {
        expect(
                "in 2 weeks",
                "eggs in 2 weeks",
                LIST_DATE,
                LIST_DATE.plusWeeks(2),
                "eggs",
                DatePhraseParser.Anchor.CURRENT_LIST_DATE,
                false,
                false);
    }

    private static void parsesNextMonth() {
        expect(
                "next month",
                "light bulbs next month",
                LIST_DATE,
                LIST_DATE.plusMonths(1),
                "light bulbs",
                DatePhraseParser.Anchor.CURRENT_LIST_DATE,
                false,
                false);
    }

    private static void parsesThisWeekend() {
        expect(
                "this weekend",
                "bread this weekend",
                LIST_DATE,
                LocalDate.of(2026, 11, 7),
                "bread",
                DatePhraseParser.Anchor.CURRENT_LIST_DATE,
                false,
                false);
    }

    private static void parsesNextShoppingDay() {
        DatePhraseParser.ParsedDate parsed =
                DatePhraseParser.parse("coffee next shopping day", LIST_DATE, TODAY).orElseThrow();
        check("next shopping day phrase", parsed.phrase().equals("next shopping day"));
        check("next shopping day item", parsed.itemText().equals("coffee"));
        check("next shopping day requires schedule", parsed.requiresSchedule());
        check("next shopping day has no date", parsed.resolvedDate().isEmpty());
        check(
                "next shopping day anchor",
                parsed.anchor() == DatePhraseParser.Anchor.SCHEDULE);
    }

    private static void ignoresNonDateText() {
        check(
                "no false positive for '3 day deodorant'",
                DatePhraseParser.parse("3 day deodorant", LIST_DATE, TODAY).isEmpty());
        check(
                "no false positive for plain item",
                DatePhraseParser.parse("paper towels", LIST_DATE, TODAY).isEmpty());
    }

    private static void requiresItemTextBeforePhrase() {
        check(
                "phrase-only text is not split",
                DatePhraseParser.parse("tomorrow", LIST_DATE, TODAY).isEmpty());
    }

    private static void handlesTrailingPunctuation() {
        DatePhraseParser.ParsedDate parsed =
                DatePhraseParser.parse("batteries tomorrow.", LIST_DATE, TODAY).orElseThrow();
        check("punctuation item", parsed.itemText().equals("batteries"));
        check("punctuation phrase", parsed.phrase().equals("tomorrow"));
        check("punctuation date", parsed.resolvedDate().orElseThrow().equals(TODAY.plusDays(1)));
    }

    private static void flagsPastResolvedDate() {
        DatePhraseParser.ParsedDate parsed =
                DatePhraseParser.parse("milk next week", LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 10))
                        .orElseThrow();
        check("past resolved date flagged", parsed.needsConfirmation());
    }

    private static void handlesMonthEndAndLeapYear() {
        expect(
                "month-end next month",
                "paper towels next month",
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 1, 1),
                "paper towels",
                DatePhraseParser.Anchor.CURRENT_LIST_DATE,
                false,
                false);
        expect(
                "leap-year next month",
                "paper towels next month",
                LocalDate.of(2028, 1, 31),
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2028, 1, 1),
                "paper towels",
                DatePhraseParser.Anchor.CURRENT_LIST_DATE,
                false,
                false);
    }

    private static void handlesWeekendOnSaturday() {
        expect(
                "weekend when list is Saturday",
                "bread this weekend",
                LocalDate.of(2026, 11, 7),
                LocalDate.of(2026, 11, 7),
                "bread",
                DatePhraseParser.Anchor.CURRENT_LIST_DATE,
                false,
                false);
    }

    private static void resolvesNearestList() {
        DatePhraseParser.NearestList result =
                DatePhraseParser.nearestList(
                        LocalDate.of(2026, 11, 5),
                        List.of(
                                LocalDate.of(2026, 11, 3),
                                LocalDate.of(2026, 11, 8),
                                LocalDate.of(2026, 11, 12)),
                        TODAY);
        check("nearest list date", result.existingListDate().orElseThrow().equals(LocalDate.of(2026, 11, 8)));
        check("nearest list does not create", !result.createList());

        DatePhraseParser.NearestList exact =
                DatePhraseParser.nearestList(
                        LocalDate.of(2026, 11, 5),
                        List.of(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 8)),
                        TODAY);
        check("exact list date", exact.existingListDate().orElseThrow().equals(LocalDate.of(2026, 11, 5)));
    }

    private static void createsListWhenNoneMatches() {
        DatePhraseParser.NearestList result =
                DatePhraseParser.nearestList(
                        LocalDate.of(2026, 11, 9),
                        List.of(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5)),
                        TODAY);
        check("create list when none matches", result.createList());
        check("create list retains target", result.targetDate().equals(LocalDate.of(2026, 11, 9)));
    }

    private static void flagsPastTargetDate() {
        DatePhraseParser.NearestList result =
                DatePhraseParser.nearestList(LocalDate.of(2026, 10, 30), List.of(), TODAY);
        check("past target needs confirmation", result.needsConfirmation());
        check("past target creates list", result.createList());
    }

    private static void expect(
            String label,
            String text,
            LocalDate listDate,
            LocalDate expectedDate,
            String expectedItem,
            DatePhraseParser.Anchor expectedAnchor,
            boolean expectedNeedsConfirmation,
            boolean expectedRequiresSchedule) {
        expect(
                label,
                text,
                listDate,
                expectedDate,
                TODAY,
                expectedItem,
                expectedAnchor,
                expectedNeedsConfirmation,
                expectedRequiresSchedule);
    }

    private static void expect(
            String label,
            String text,
            LocalDate listDate,
            LocalDate expectedDate,
            LocalDate today,
            String expectedItem,
            DatePhraseParser.Anchor expectedAnchor,
            boolean expectedNeedsConfirmation,
            boolean expectedRequiresSchedule) {
        Optional<DatePhraseParser.ParsedDate> maybe = DatePhraseParser.parse(text, listDate, today);
        if (maybe.isEmpty()) {
            fail(label + ": expected a parse result");
            return;
        }
        DatePhraseParser.ParsedDate parsed = maybe.get();
        check(label + " item", parsed.itemText().equals(expectedItem));
        check(label + " date", parsed.resolvedDate().orElseThrow().equals(expectedDate));
        check(label + " anchor", parsed.anchor() == expectedAnchor);
        check(label + " confirmation", parsed.needsConfirmation() == expectedNeedsConfirmation);
        check(label + " schedule", parsed.requiresSchedule() == expectedRequiresSchedule);
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS " + label);
        } else {
            fail(label);
        }
    }

    private static void fail(String label) {
        failed++;
        System.out.println("FAIL " + label);
    }
}
