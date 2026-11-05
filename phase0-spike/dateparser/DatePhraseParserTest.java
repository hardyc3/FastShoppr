package dateparser;

import java.time.LocalDate;
import java.util.List;

public final class DatePhraseParserTest {
    static int failures = 0;

    static void check(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        System.out.printf("%-40s %s%n", name, ok ? "PASS" : "FAIL expected=" + expected + " actual=" + actual);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        // Fixed clock: Tuesday 2026-11-03
        LocalDate t = LocalDate.of(2026, 11, 3);
        DatePhraseParser p = new DatePhraseParser(t);

        check("tomorrow", LocalDate.of(2026, 11, 4), p.parse("tomorrow").get().date());
        check("in 3 days", LocalDate.of(2026, 11, 6), p.parse("in 3 days").get().date());
        check("in a day singular", LocalDate.of(2026, 11, 4), p.parse("in 1 day").get().date());
        check("wait a week", LocalDate.of(2026, 11, 10), p.parse("wait a week").get().date());
        check("in 2 weeks", LocalDate.of(2026, 11, 17), p.parse("in 2 weeks").get().date());
        check("next week -> Mon, confirm", LocalDate.of(2026, 11, 9), p.parse("next week").get().date());
        check("next week precision", DatePhraseParser.Precision.NEEDS_CONFIRMATION, p.parse("next week").get().precision());
        check("next month -> same day, confirm", LocalDate.of(2026, 12, 3), p.parse("next month").get().date());
        check("next month precision", DatePhraseParser.Precision.NEEDS_CONFIRMATION, p.parse("next month").get().precision());
        check("this weekend -> Sat 2026-11-07", LocalDate.of(2026, 11, 7), p.parse("this weekend").get().date());
        check("this weekend precision", DatePhraseParser.Precision.NEEDS_CONFIRMATION, p.parse("this weekend").get().precision());
        check("tomorrow precision exact", DatePhraseParser.Precision.EXACT, p.parse("tomorrow").get().precision());
        check("unparseable -> empty", true, p.parse("whenever").isEmpty());

        // Sunday edge: 2026-11-08 is a Sunday; "this weekend" -> next Saturday 2026-11-14 (Saturday not same-day)
        DatePhraseParser sun = new DatePhraseParser(LocalDate.of(2026, 11, 8));
        check("this weekend on Sunday -> 2026-11-14", LocalDate.of(2026, 11, 14), sun.parse("this weekend").get().date());
        // Saturday edge: today is Saturday -> today
        DatePhraseParser sat = new DatePhraseParser(LocalDate.of(2026, 11, 7));
        check("this weekend on Saturday -> today", LocalDate.of(2026, 11, 7), sat.parse("this weekend").get().date());
        // Month-end clamp: 2026-01-31 + 1 month -> 2026-02-28 (java.time clamps)
        DatePhraseParser jan = new DatePhraseParser(LocalDate.of(2026, 1, 31));
        check("next month clamps to Feb 28", LocalDate.of(2026, 2, 28), jan.parse("next month").get().date());

        // List mapping: nearest on-or-after
        List<LocalDate> lists = List.of(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 20));
        check("nearest >= 2026-11-06 -> 2026-11-12", LocalDate.of(2026, 11, 12), DatePhraseParser.nearestListOnOrAfter(LocalDate.of(2026, 11, 6), lists));
        check("nearest >= 2026-11-05 -> 2026-11-05", LocalDate.of(2026, 11, 5), DatePhraseParser.nearestListOnOrAfter(LocalDate.of(2026, 11, 5), lists));
        check("nearest >= 2026-11-21 -> null", null, DatePhraseParser.nearestListOnOrAfter(LocalDate.of(2026, 11, 21), lists));

        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }
}
