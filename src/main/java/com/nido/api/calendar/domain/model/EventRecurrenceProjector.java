package com.nido.api.calendar.domain.model;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Expands a recurring event series over a window, in memory, never persisting anything.
 *
 * <p>The date arithmetic is the same anchored sequence as Tasks' {@code RecurrenceScheduler} and
 * Finance's {@code RecurrenceProjector}: period N — the week, month or year N intervals after the
 * anchor's — is re-derived from the anchor every time, and gives its dates by the series' rule, so a
 * monthly series anchored on the 31st lands on Feb 28 and then back on Mar 31 rather than drifting to
 * Mar 28. This is the third copy of that arithmetic in the codebase, kept deliberately — each bounded
 * context owns its domain.
 *
 * <p>What is new here is that a window is not simply a filter on the slot dates. Two things
 * remove a slot, and one thing extends the search backwards:
 * <ul>
 *   <li>an exclusion cancels the slot outright;</li>
 *   <li>a detached instance takes the slot over, and is returned by the plain event query
 *       instead — so producing it here as well would show the same event twice;</li>
 *   <li>a multi-day occurrence starting before {@code from} may still be running inside the
 *       window, so the scan starts {@code durationDays} earlier.</li>
 * </ul>
 *
 * <p>Because nothing is ever materialized, past and future are perfectly symmetric: there is no
 * horizon to push forward, no backlog to catch up, and no job to run.
 */
public final class EventRecurrenceProjector {

    /**
     * Widest window a single read may ask for. A month view asks for 42 days; this leaves room
     * for a future year view while keeping the endpoint from becoming a full export.
     */
    public static final int MAX_WINDOW_DAYS = 366;

    private EventRecurrenceProjector() {}

    /**
     * The earliest slot that can still be running inside a window starting at {@code from}: an
     * occurrence lasting several days may have begun before it. Whoever looks up the slots a series
     * has cancelled or detached must look this far back too — or an occurrence that began before
     * the window comes back from the dead, or shows twice.
     *
     * <p>Never before the day the series is shown from: those occurrences belong to the series it carried
     * on from, which shows them itself. That also keeps a duration stored before occurrences were bounded
     * from reaching back past the first date the database holds.
     */
    public static LocalDate scanFrom(RecurringEventSeries series, LocalDate from) {
        LocalDate earliest = from.minusDays(series.durationDays());
        return earliest.isBefore(series.shownFrom()) ? series.shownFrom() : earliest;
    }

    /**
     * Whether the series lies on both sides of {@code day}: it has an occurrence before it and is not
     * over by then. Editing or deleting such a series leaves what came before {@code day} as it was.
     */
    public static boolean runsAcross(RecurringEventSeries series, LocalDate day) {
        boolean begun = firstOccurrence(series).filter(first -> first.isBefore(day)).isPresent();
        boolean over = series.endDate() != null && series.endDate().isBefore(day);
        return begun && !over;
    }

    /**
     * The occurrence of {@code series} nearest to {@code date} — the later of two at the same distance —
     * among those less than one interval away; none if the series has no occurrence that close.
     */
    public static Optional<LocalDate> nearestSlot(RecurringEventSeries series, LocalDate date) {
        long within = EventScheduleValidator.shortestIntervalDays(series) - 1;
        LocalDate from = date.minusDays(within);
        return slotsBetween(series, from, date.plusDays(within)).stream()
            .filter(slot -> !slot.isBefore(from))
            .min(Comparator.comparingLong((LocalDate slot) -> Math.abs(ChronoUnit.DAYS.between(date, slot)))
                .thenComparing(Comparator.reverseOrder()));
    }

    /**
     * The series' occurrences inside {@code [from, to]}, minus the slots that are cancelled and
     * minus the slots a detached instance has taken over.
     */
    public static List<CalendarOccurrence> project(RecurringEventSeries series, Set<LocalDate> excludedSlots,
                                                   Set<LocalDate> detachedSlots, LocalDate from, LocalDate to) {
        List<CalendarOccurrence> produced = new ArrayList<>();
        for (LocalDate slot : slotsBetween(series, from, to)) {
            if (excludedSlots.contains(slot) || detachedSlots.contains(slot)) {
                continue;
            }
            produced.add(new CalendarOccurrence(
                CalendarSourceType.EVENT, CalendarOccurrence.projectedId(series.id(), slot),
                series.id(), slot, false, series.title(), series.description(), series.location(), series.allDay(),
                slot, series.startTime(), slot.plusDays(series.durationDays()), series.endTime(),
                series.color(), series.participantIds()));
        }
        return produced;
    }

    /**
     * The dates of the series' period {@code period} — the week, month or year that many intervals after the
     * anchor's —, in order, before any bound is applied: a week may hold several, a month or a year holds one.
     *
     * <p>None past the last date a calendar holds, the year 999,999,999: a series stored before its interval was
     * bounded, every billion years, has no occurrence there — rather than failing every read of its space.
     */
    static List<LocalDate> datesOfPeriod(RecurringEventSeries series, long period) {
        // Not reachable through the API (the record and the CHECK constraint both refuse it), but the scans below
        // never advance and loop forever if it ever were — fail fast instead.
        if (series.intervalCount() < 1) {
            throw new IllegalArgumentException("intervalCount must be at least 1, got " + series.intervalCount());
        }
        LocalDate anchor = series.anchorDate();
        try {
            long steps = Math.multiplyExact(series.intervalCount(), period);
            return switch (series.intervalType()) {
                case DAILY -> List.of(anchor.plusDays(steps));
                case WEEKLY -> {
                    LocalDate monday = anchor.with(DayOfWeek.MONDAY).plusWeeks(steps);
                    yield series.weekdays().stream().map(day -> monday.plusDays(day.getValue() - 1L)).toList();
                }
                case MONTHLY -> List.of(series.dayRule().in(YearMonth.from(anchor).plusMonths(steps), anchor));
                case YEARLY -> List.of(series.dayRule().in(YearMonth.from(anchor).plusYears(steps), anchor));
            };
        } catch (DateTimeException | ArithmeticException pastTheLastDate) {
            return List.of();
        }
    }

    /** The series' first occurrence, if it has any: on or after the day it is shown from. */
    public static Optional<LocalDate> firstOccurrence(RecurringEventSeries series) {
        LocalDate from = series.shownFrom();
        // The period reaching `from` holds a date on or after it, by definition — unless it lies past the last date.
        return datesOfPeriod(series, firstPeriodReaching(series, from)).stream()
            .filter(date -> !date.isBefore(from))
            .findFirst()
            .filter(date -> series.endDate() == null || !date.isAfter(series.endDate()));
    }

    /** Every slot of {@code series} overlapping {@code [from, to]}, in ascending order. */
    public static List<LocalDate> slotsBetween(RecurringEventSeries series, LocalDate from, LocalDate to) {
        // The anchor's week may hold days before the anchor, which are not occurrences: none is before scanFrom.
        LocalDate scanFrom = scanFrom(series, from);
        LocalDate lastAllowed = series.endDate() == null || series.endDate().isAfter(to) ? to : series.endDate();

        List<LocalDate> slots = new ArrayList<>();
        for (long period = firstPeriodReaching(series, scanFrom); ; period++) {
            List<LocalDate> dates = datesOfPeriod(series, period);
            if (dates.isEmpty() || dates.getFirst().isAfter(lastAllowed)) {
                return slots;
            }
            for (LocalDate date : dates) {
                if (!date.isBefore(scanFrom) && !date.isAfter(lastAllowed)) {
                    slots.add(date);
                }
            }
        }
    }

    /**
     * The first period whose last date falls on or after {@code target}, found arithmetically rather than by stepping
     * from period 0 — so the cost depends on how many dates come back, never on how old the anchor is. The estimate can
     * be off by one (a month's day may fall either side of the target's), so two bounded corrections settle it.
     */
    private static long firstPeriodReaching(RecurringEventSeries series, LocalDate target) {
        LocalDate anchor = series.anchorDate();
        if (!target.isAfter(anchor)) {
            return 0;
        }
        long count = series.intervalCount();
        long estimate = switch (series.intervalType()) {
            case DAILY -> ChronoUnit.DAYS.between(anchor, target) / count;
            case WEEKLY -> ChronoUnit.WEEKS.between(anchor.with(DayOfWeek.MONDAY), target.with(DayOfWeek.MONDAY)) / count;
            case MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(target)) / count;
            case YEARLY -> ((long) target.getYear() - anchor.getYear()) / count;
        };
        long period = Math.max(0L, estimate);
        while (period > 0 && !lastDateOf(series, period - 1).isBefore(target)) {
            period--;
        }
        while (lastDateOf(series, period).isBefore(target)) {
            period++;
        }
        return period;
    }

    /** The last date of a period; one past the last date a calendar holds comes after every date. */
    private static LocalDate lastDateOf(RecurringEventSeries series, long period) {
        List<LocalDate> dates = datesOfPeriod(series, period);
        return dates.isEmpty() ? LocalDate.MAX : dates.getLast();
    }
}
