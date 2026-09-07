package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Computes a recurring series' due dates as a fixed calendar sequence
 * anchored to its start date — never relative to when each occurrence
 * actually gets completed, so a late or early completion never shifts the
 * schedule. Occurrence 0 is the anchor date itself; occurrence N is N
 * intervals after it. Monthly anchoring on a day a target month doesn't
 * have (e.g. the 31st) clamps to that month's last day, but every later
 * occurrence is still computed from the original anchor day, not from the
 * clamped value — so a Jan-31 anchor lands on Feb 28 and then back on
 * Mar 31, never drifting to Mar 28.
 */
public final class RecurrenceScheduler {

    private RecurrenceScheduler() {}

    public static LocalDate nextDueDate(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount, int occurrenceNumber) {
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
     * Shifts a concrete date backward by a single interval — used to find where a
     * materialized occurrence's lead-time visibility window opens. Unlike
     * {@link #nextDueDate}, this is a one-off shift of an arbitrary date, not a
     * repeated re-derivation from a fixed anchor, so the JDK's own end-of-month
     * clamping (e.g. {@code LocalDate.of(2026,3,31).minusMonths(1)} → 2026-02-28)
     * is already correct with no extra logic needed.
     */
    public static LocalDate minus(LocalDate date, RecurrenceInterval intervalType, int intervalCount) {
        return switch (intervalType) {
            case DAILY -> date.minusDays(intervalCount);
            case WEEKLY -> date.minusWeeks(intervalCount);
            case MONTHLY -> date.minusMonths(intervalCount);
            case YEARLY -> date.minusYears(intervalCount);
        };
    }
}
