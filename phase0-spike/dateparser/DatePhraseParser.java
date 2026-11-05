package dateparser;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 0 spike: deterministic date-phrase parser.
 * Pure Java, no Android deps. Fixed clock injected for tests.
 */
public final class DatePhraseParser {
    private final LocalDate today;

    public DatePhraseParser(LocalDate today) {
        this.today = today;
    }

    public enum Precision { EXACT, NEEDS_CONFIRMATION }

    public record ParsedDate(LocalDate date, Precision precision, String rule) {}

    public record ListMatch(LocalDate shoppingDate, boolean created) {}

    public Optional<ParsedDate> parse(String raw) {
        if (raw == null) return Optional.empty();
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (s.isEmpty()) return Optional.empty();

        if (s.equals("today")) return opt(today, Precision.EXACT, "today");
        if (s.equals("tomorrow")) return opt(today.plusDays(1), Precision.EXACT, "tomorrow");

        Matcher days = Pattern.compile("^in (\\d+) days?$").matcher(s);
        if (days.matches()) return opt(today.plusDays(Long.parseLong(days.group(1))), Precision.EXACT, "in N days");

        if (s.equals("wait a week") || s.equals("in a week") || s.equals("next week in a week"))
            return opt(today.plusDays(7), Precision.EXACT, "+7 days");

        Matcher weeks = Pattern.compile("^in (\\d+) weeks?$").matcher(s);
        if (weeks.matches()) return opt(today.plusWeeks(Long.parseLong(weeks.group(1))), Precision.EXACT, "in N weeks");

        if (s.equals("next week")) {
            // Ambiguous: could mean +7 days or Monday of next ISO week.
            LocalDate mondayNext = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            return opt(mondayNext, Precision.NEEDS_CONFIRMATION, "next week -> next Monday (ambiguous vs +7d)");
        }

        if (s.equals("next month")) {
            // Ambiguous: same day next month vs 1st of next month.
            LocalDate sameDayNextMonth = today.plusMonths(1);
            return opt(sameDayNextMonth, Precision.NEEDS_CONFIRMATION, "next month -> same day next month (ambiguous vs 1st)");
        }

        if (s.equals("this weekend")) {
            // Saturday of the current week; if today is Sunday, treat as today-ish -> next Saturday? Rule: upcoming Saturday, today if Saturday.
            LocalDate sat = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            return opt(sat, Precision.NEEDS_CONFIRMATION, "this weekend -> Saturday (ambiguous vs Sunday/range)");
        }

        Matcher on = Pattern.compile("^on (\\d{4}-\\d{2}-\\d{2})$").matcher(s);
        if (on.matches()) return opt(LocalDate.parse(on.group(1)), Precision.EXACT, "iso date");

        return Optional.empty();
    }

    /** Nearest list with shoppingDate on or after target. Null if none. */
    public static LocalDate nearestListOnOrAfter(LocalDate target, java.util.List<LocalDate> listDates) {
        return listDates.stream()
                .filter(d -> !d.isBefore(target))
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private static Optional<ParsedDate> opt(LocalDate d, Precision p, String rule) {
        return Optional.of(new ParsedDate(d, p, rule));
    }
}
