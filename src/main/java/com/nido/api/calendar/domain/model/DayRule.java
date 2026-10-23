package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

/**
 * Which day of a month a monthly or yearly series falls on. Like Google, the series stores only the rule: the day, the
 * weekday and its rank are read off the anchor, so a rule and its anchor can never disagree.
 *
 * <p>A rank stops at the fourth: every month has a first to a fourth of each weekday, and a last one, so each period
 * has exactly one occurrence. A "fifth Tuesday" is said "last", and one imported as such is unfolded instead.
 */
public enum DayRule {
    /** The anchor's day of the month — the 31st becomes the 30th or the 28th in shorter months, as it always did. */
    SAME_DAY,
    /** The anchor's weekday, at the anchor's rank in its month: the second Tuesday. */
    WEEKDAY_OF_MONTH,
    /** The anchor's weekday, the last of its month: the last Friday. */
    LAST_WEEKDAY_OF_MONTH,
    /** The last day of the month, whatever its length. */
    LAST_DAY_OF_MONTH;

    /** Whether this rule can be read off {@code anchor}. */
    public boolean fits(LocalDate anchor) {
        return switch (this) {
            case SAME_DAY -> true;
            case WEEKDAY_OF_MONTH -> anchor.getDayOfMonth() <= 28;
            case LAST_WEEKDAY_OF_MONTH -> anchor.getDayOfMonth() > anchor.lengthOfMonth() - 7;
            case LAST_DAY_OF_MONTH -> anchor.getDayOfMonth() == anchor.lengthOfMonth();
        };
    }

    /** The day this rule gives in {@code month}, read off {@code anchor}. */
    public LocalDate in(YearMonth month, LocalDate anchor) {
        return switch (this) {
            case SAME_DAY -> month.atDay(Math.min(anchor.getDayOfMonth(), month.lengthOfMonth()));
            case WEEKDAY_OF_MONTH -> month.atDay(1).with(
                TemporalAdjusters.dayOfWeekInMonth((anchor.getDayOfMonth() - 1) / 7 + 1, anchor.getDayOfWeek()));
            case LAST_WEEKDAY_OF_MONTH -> month.atEndOfMonth().with(TemporalAdjusters.lastInMonth(anchor.getDayOfWeek()));
            case LAST_DAY_OF_MONTH -> month.atEndOfMonth();
        };
    }
}
