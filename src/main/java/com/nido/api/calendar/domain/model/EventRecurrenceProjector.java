package com.nido.api.calendar.domain.model;

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
 * Finance's {@code RecurrenceProjector}: occurrence N is N intervals after the anchor, re-derived
 * from the anchor every time, so a monthly series anchored on the 31st lands on Feb 28 and then
 * back on Mar 31 rather than drifting to Mar 28. This is the third copy of that arithmetic in the
 * codebase, kept deliberately — each bounded context owns its domain.
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

    public static LocalDate occurrenceDate(LocalDate anchorDate, RecurrenceInterval intervalType,
                                           int intervalCount, int occurrenceNumber) {
        // Not reachable through the API (the record and the CHECK constraint both refuse it), but
        // the scan below never advances and loops forever if it ever were — fail fast instead.
        if (intervalCount < 1) {
            throw new IllegalArgumentException("intervalCount must be at least 1, got " + intervalCount);
        }
        long steps = (long) intervalCount * occurrenceNumber;
        return switch (intervalType) {
            case DAILY -> anchorDate.plusDays(steps);
            case WEEKLY -> anchorDate.plusWeeks(steps);
            case MONTHLY -> {
                YearMonth targetMonth = YearMonth.from(anchorDate).plusMonths(steps);
                int day = Math.min(anchorDate.getDayOfMonth(), targetMonth.lengthOfMonth());
                yield targetMonth.atDay(day);
            }
            case YEARLY -> anchorDate.plusYears(steps);
        };
    }

    /**
     * The earliest slot that can still be running inside a window starting at {@code from}: an
     * occurrence lasting several days may have begun before it. Whoever looks up the slots a series
     * has cancelled or detached must look this far back too — or an occurrence that began before
     * the window comes back from the dead, or shows twice.
     */
    public static LocalDate scanFrom(RecurringEventSeries series, LocalDate from) {
        return from.minusDays(series.durationDays());
    }

    /** The series' first occurrence, if it has any: on or after the day it is shown from. */
    public static Optional<LocalDate> firstOccurrence(RecurringEventSeries series) {
        LocalDate first = occurrenceDate(series.anchorDate(), series.intervalType(), series.intervalCount(),
            firstIndexOnOrAfter(series, series.shownFrom()));
        return series.endDate() != null && first.isAfter(series.endDate()) ? Optional.empty() : Optional.of(first);
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
        long within = EventScheduleValidator.shortestIntervalDays(series.intervalType(), series.intervalCount()) - 1;
        LocalDate from = date.minusDays(within);
        return slotsBetween(series, from, date.plusDays(within)).stream()
            .filter(slot -> !slot.isBefore(from))
            .min(Comparator.comparingLong((LocalDate slot) -> Math.abs(ChronoUnit.DAYS.between(date, slot)))
                .thenComparing(Comparator.reverseOrder()));
    }

    /** Every slot of {@code series} overlapping {@code [from, to]}, in ascending order. */
    public static List<LocalDate> slotsBetween(RecurringEventSeries series, LocalDate from, LocalDate to) {
        // Nothing before the day the series is shown from: those occurrences belong to the series it
        // carried on from, which shows them itself.
        LocalDate scanFrom = scanFrom(series, from);
        if (scanFrom.isBefore(series.shownFrom())) {
            scanFrom = series.shownFrom();
        }
        LocalDate lastAllowed = series.endDate() == null || series.endDate().isAfter(to) ? to : series.endDate();

        List<LocalDate> slots = new ArrayList<>();
        int n = firstIndexOnOrAfter(series, scanFrom);
        while (true) {
            LocalDate slot = occurrenceDate(series.anchorDate(), series.intervalType(), series.intervalCount(), n);
            if (slot.isAfter(lastAllowed)) {
                return slots;
            }
            slots.add(slot);
            n++;
        }
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
     * Index of the first occurrence falling on or after {@code target}, found arithmetically
     * rather than by stepping from occurrence 0 — so the cost depends on how many dates come back,
     * never on how old the anchor is. The division can be off by one, because month-end clamping
     * means the estimate captures the calendar month rather than the exact day, so two bounded
     * corrections settle it.
     */
    private static int firstIndexOnOrAfter(RecurringEventSeries series, LocalDate target) {
        LocalDate anchor = series.anchorDate();
        if (!target.isAfter(anchor)) {
            return 0;
        }
        long estimate = switch (series.intervalType()) {
            case DAILY -> ChronoUnit.DAYS.between(anchor, target) / series.intervalCount();
            case WEEKLY -> ChronoUnit.WEEKS.between(anchor, target) / series.intervalCount();
            case MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(target)) / series.intervalCount();
            case YEARLY -> ChronoUnit.YEARS.between(anchor, target) / series.intervalCount();
        };
        int n = (int) Math.max(0L, Math.min(estimate, Integer.MAX_VALUE - 2L));
        while (n > 0 && !occurrenceDate(anchor, series.intervalType(), series.intervalCount(), n - 1).isBefore(target)) {
            n--;
        }
        while (occurrenceDate(anchor, series.intervalType(), series.intervalCount(), n).isBefore(target)) {
            n++;
        }
        return n;
    }
}
