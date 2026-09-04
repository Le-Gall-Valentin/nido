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
        };
    }
}
