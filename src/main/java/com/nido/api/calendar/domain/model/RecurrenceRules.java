package com.nido.api.calendar.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Between a nido series and an iCalendar RRULE (RFC 5545 § 3.3.10).
 *
 * <p>{@link #toRrule} is exact: every rule nido has an RRULE that gives the same days. nido's "same day" caps the 29th,
 * 30th and 31st at the end of shorter months, which a plain {@code BYMONTHDAY} does not (RFC 5545 skips the months
 * without that day), hence {@code BYMONTHDAY=28,29,30;BYSETPOS=-1} and {@code BYMONTHDAY=-1}.
 *
 * <p>{@link #candidateFor} is a guess: the import builds the series it suggests, then keeps it only if nido projects
 * exactly the occurrences ical4j expands — any doubt here is settled there.
 */
public final class RecurrenceRules {

    private static final DateTimeFormatter UTC_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /**
     * How many steps of the series' rhythm a capped day looks back through for a period that has it: 48 periods of its
     * interval — 48 months for a monthly series, 48 × 3 for one every third month, 48 years for a yearly one.
     */
    private static final int BACK_ANCHOR_TRIES = 48;

    /** A nido rule that may give an RRULE's occurrences, anchored as nido needs it. */
    public record Candidate(RecurrenceInterval intervalType, int intervalCount, Set<DayOfWeek> weekdays, DayRule dayRule,
                            LocalDate anchorDate, LocalDate startsOn) {}

    private RecurrenceRules() {}

    /** The RRULE of {@code series}, without its "RRULE:" name; the times of a timed series are those of {@code zone}. */
    public static String toRrule(RecurringEventSeries series, ZoneId zone) {
        StringBuilder rule = new StringBuilder("FREQ=").append(series.intervalType().name());
        if (series.intervalCount() > 1) {
            rule.append(";INTERVAL=").append(series.intervalCount());
        }
        LocalDate anchor = series.anchorDate();
        switch (series.intervalType()) {
            case DAILY -> { }
            case WEEKLY -> rule.append(";BYDAY=").append(Weekdays.toCodes(series.weekdays())).append(";WKST=MO");
            case MONTHLY -> rule.append(dayPart(series.dayRule(), anchor, false));
            case YEARLY -> rule.append(";BYMONTH=").append(anchor.getMonthValue()).append(dayPart(series.dayRule(), anchor, true));
        }
        if (series.endDate() != null) {
            rule.append(";UNTIL=").append(series.allDay()
                ? series.endDate().format(DateTimeFormatter.BASIC_ISO_DATE)
                // The last instant of the last day an occurrence may start on, in the series' zone (RFC 5545: UTC).
                : series.endDate().plusDays(1).atStartOfDay(zone).minusSeconds(1)
                    .withZoneSameInstant(ZoneOffset.UTC).format(UTC_DATE_TIME));
        }
        return rule.toString();
    }

    private static String dayPart(DayRule rule, LocalDate anchor, boolean yearly) {
        int day = anchor.getDayOfMonth();
        return switch (rule) {
            case SAME_DAY -> {
                if (yearly && anchor.getMonth() == Month.FEBRUARY && day == 29) {
                    yield ";BYMONTHDAY=28,29;BYSETPOS=-1";
                }
                if (yearly || day <= 28) {
                    yield ";BYMONTHDAY=" + day;
                }
                if (day == 31) {
                    yield ";BYMONTHDAY=-1";
                }
                yield ";BYMONTHDAY=" + IntStream.rangeClosed(28, day).mapToObj(String::valueOf).collect(Collectors.joining(","))
                    + ";BYSETPOS=-1";
            }
            case WEEKDAY_OF_MONTH -> ";BYDAY=" + ((day - 1) / 7 + 1) + Weekdays.code(anchor.getDayOfWeek());
            case LAST_WEEKDAY_OF_MONTH -> ";BYDAY=-1" + Weekdays.code(anchor.getDayOfWeek());
            case LAST_DAY_OF_MONTH -> ";BYMONTHDAY=-1";
        };
    }

    /** A nido rule that may give the occurrences of {@code r} from {@code first}, its first occurrence; none if it cannot. */
    public static Optional<Candidate> candidateFor(IcalRecurrence r, LocalDate first) {
        if (r.hasOtherParts() || r.frequency() == null || r.interval() > EventScheduleValidator.MAX_INTERVAL
            || r.byMonthDay().stream().anyMatch(d -> d == 0 || d < -31 || d > 31)
            || r.byMonth().stream().anyMatch(m -> m < 1 || m > 12)) {
            return Optional.empty();
        }
        int count = Math.max(1, r.interval());
        return switch (r.frequency()) {
            case "DAILY" -> daily(r, count, first);
            case "WEEKLY" -> weekly(r, count, first);
            case "MONTHLY" -> monthly(r, count, first);
            case "YEARLY" -> yearly(r, count, first);
            default -> Optional.empty();
        };
    }

    private static Optional<Candidate> daily(IcalRecurrence r, int count, LocalDate first) {
        if (!r.byMonthDay().isEmpty() || !r.byMonth().isEmpty() || !r.bySetPos().isEmpty()) {
            return Optional.empty();
        }
        if (r.byDay().isEmpty()) {
            return Optional.of(new Candidate(RecurrenceInterval.DAILY, count, null, DayRule.SAME_DAY, first, null));
        }
        // "Every working day" is a daily rule on some weekdays: a weekly series on those days, when it runs every day.
        return count == 1 ? weekly(r, 1, first) : Optional.empty();
    }

    private static Optional<Candidate> weekly(IcalRecurrence r, int count, LocalDate first) {
        if (!r.byMonthDay().isEmpty() || !r.byMonth().isEmpty() || !r.bySetPos().isEmpty()
            || r.byDay().stream().anyMatch(day -> day.offset() != 0)) {
            return Optional.empty();
        }
        Set<DayOfWeek> days = r.byDay().isEmpty()
            ? Weekdays.of(first.getDayOfWeek())
            : Weekdays.copyOf(r.byDay().stream().map(IcalRecurrence.ByDay::day).toList());
        return days.contains(first.getDayOfWeek())
            ? Optional.of(new Candidate(RecurrenceInterval.WEEKLY, count, days, DayRule.SAME_DAY, first, null))
            : Optional.empty();
    }

    private static Optional<Candidate> monthly(IcalRecurrence r, int count, LocalDate first) {
        if (!r.byMonth().isEmpty()) {
            return Optional.empty();
        }
        return byDayOfMonth(r, RecurrenceInterval.MONTHLY, count, first);
    }

    private static Optional<Candidate> yearly(IcalRecurrence r, int count, LocalDate first) {
        if (r.byMonth().size() > 1 || r.byMonth().size() == 1 && r.byMonth().getFirst() != first.getMonthValue()) {
            return Optional.empty();
        }
        return byDayOfMonth(r, RecurrenceInterval.YEARLY, count, first);
    }

    private static Optional<Candidate> byDayOfMonth(IcalRecurrence r, RecurrenceInterval type, int count, LocalDate first) {
        List<Integer> monthDays = r.byMonthDay();
        boolean lastOfSet = r.bySetPos().equals(List.of(-1));
        if (r.byDay().isEmpty() && monthDays.isEmpty() && r.bySetPos().isEmpty()) {
            return sameDay(type, count, first);
        }
        if (r.byDay().size() == 1 && monthDays.isEmpty() && r.bySetPos().isEmpty()) {
            IcalRecurrence.ByDay byDay = r.byDay().getFirst();
            if (byDay.day() != first.getDayOfWeek()) {
                return Optional.empty();
            }
            if (byDay.offset() == -1 && DayRule.LAST_WEEKDAY_OF_MONTH.fits(first)) {
                return Optional.of(new Candidate(type, count, null, DayRule.LAST_WEEKDAY_OF_MONTH, first, null));
            }
            if (byDay.offset() >= 1 && byDay.offset() <= 4 && (first.getDayOfMonth() - 1) / 7 + 1 == byDay.offset()) {
                return Optional.of(new Candidate(type, count, null, DayRule.WEEKDAY_OF_MONTH, first, null));
            }
            return Optional.empty();
        }
        if (monthDays.equals(List.of(-1)) && r.byDay().isEmpty() && r.bySetPos().isEmpty()) {
            if (first.getDayOfMonth() == 31) {
                return sameDay(type, count, first);
            }
            return DayRule.LAST_DAY_OF_MONTH.fits(first)
                ? Optional.of(new Candidate(type, count, null, DayRule.LAST_DAY_OF_MONTH, first, null))
                : Optional.empty();
        }
        if (monthDays.size() == 1 && monthDays.getFirst() == first.getDayOfMonth() && r.byDay().isEmpty() && r.bySetPos().isEmpty()) {
            return sameDay(type, count, first);
        }
        // "The 30th, or the last day of a shorter month": 28,29,30 with the last of them.
        if (lastOfSet && r.byDay().isEmpty() && monthDays.size() >= 2 && monthDays.getFirst() == 28
            && monthDays.equals(IntStream.rangeClosed(28, monthDays.getLast()).boxed().toList())) {
            return cappedDay(type, count, monthDays.getLast(), first);
        }
        return Optional.empty();
    }

    private static Optional<Candidate> sameDay(RecurrenceInterval type, int count, LocalDate first) {
        return Optional.of(new Candidate(type, count, null, DayRule.SAME_DAY, first, null));
    }

    /**
     * The {@code day}th of the month, or its last day when shorter. Its first occurrence may be capped — 28 February for
     * "the 30th" —, and nido reads the day off the anchor: the anchor is then the last month before it that has that
     * day, in the series' rhythm, and the series is shown from its first occurrence.
     */
    private static Optional<Candidate> cappedDay(RecurrenceInterval type, int count, int day, LocalDate first) {
        if (first.getDayOfMonth() == day) {
            return sameDay(type, count, first);
        }
        if (first.getDayOfMonth() != first.lengthOfMonth()) {
            return Optional.empty();
        }
        for (int k = 1; k <= BACK_ANCHOR_TRIES; k++) {
            YearMonth month = type == RecurrenceInterval.MONTHLY
                ? YearMonth.from(first).minusMonths((long) k * count)
                : YearMonth.from(first).minusYears((long) k * count);
            if (month.lengthOfMonth() >= day && (type == RecurrenceInterval.MONTHLY || Year.isLeap(month.getYear()) || day <= 28)) {
                return Optional.of(new Candidate(type, count, null, DayRule.SAME_DAY, month.atDay(day), first));
            }
        }
        return Optional.empty();
    }
}
