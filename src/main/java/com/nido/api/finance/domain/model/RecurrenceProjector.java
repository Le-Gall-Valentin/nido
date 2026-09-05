package com.nido.api.finance.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes a recurring series' occurrence dates as a fixed calendar
 * sequence anchored to its start date — the same anchored-date math as
 * Tasks' {@code RecurrenceScheduler}, but exposed as a range query rather
 * than a single next-date lookup: Finance never pre-generates future rows,
 * so both lazy materialization (occurrences up to today) and month-ahead
 * projection (occurrences in a future month, never persisted) go through
 * {@link #occurrencesBetween}.
 */
public final class RecurrenceProjector {

    private RecurrenceProjector() {}

    public static LocalDate occurrenceDate(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount, int occurrenceNumber) {
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

    /** Every occurrence date in {@code [from, to]}, ascending, stopping at {@code endDate} when one is set. */
    public static List<LocalDate> occurrencesBetween(
            LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount, LocalDate endDate,
            LocalDate from, LocalDate to) {
        List<LocalDate> result = new ArrayList<>();
        int n = 0;
        LocalDate date = occurrenceDate(anchorDate, intervalType, intervalCount, n);
        while (date.isBefore(from)) {
            n++;
            date = occurrenceDate(anchorDate, intervalType, intervalCount, n);
        }
        while (!date.isAfter(to) && (endDate == null || !date.isAfter(endDate))) {
            result.add(date);
            n++;
            date = occurrenceDate(anchorDate, intervalType, intervalCount, n);
        }
        return result;
    }
}
