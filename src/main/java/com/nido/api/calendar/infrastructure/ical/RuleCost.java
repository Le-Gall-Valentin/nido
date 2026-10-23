package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.EventScheduleValidator;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import net.fortuna.ical4j.util.Configurator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * What expanding a rule may cost ical4j, in candidates generated: the periods it may walk times the candidates one period
 * may give, an upper bound. {@link BoundedExpansion} charges it to the budget of a reading before ical4j is asked anything.
 */
final class RuleCost {

    /** No count of candidates is taken past the budget of a whole import: a larger one is refused, and no product of them can overflow. */
    static final long CAP = BoundedExpansion.BUDGET;

    /**
     * ical4j doubles an {@code int} multiplier to skip from DTSTART to the window; it overflows past 2^30 and the skip
     * then never ends. A rule needing this many steps from DTSTART is refused well before.
     */
    private static final long OVERFLOWING_STEPS = 1L << 29;

    private RuleCost() {}

    /**
     * Why no window makes a rule one to expand: BAD_RULE for one nido cannot hold (no frequency, an interval nido cannot
     * say, a BY list repeating or longer than its values, hours of a date), TOO_HEAVY for seconds or minutes. Empty otherwise.
     */
    static Optional<String> refusal(Recur<Temporal> recur, boolean dateStart) {
        Frequency frequency = recur.getFrequency();
        if (frequency == null || recur.getInterval() > EventScheduleValidator.MAX_INTERVAL || !distinctAndShort(recur)
            || dateStart && (frequency == Frequency.HOURLY || !recur.getHourList().isEmpty()
                || !recur.getMinuteList().isEmpty() || !recur.getSecondList().isEmpty())) {
            return Optional.of("BAD_RULE");
        }
        if (frequency == Frequency.SECONDLY || frequency == Frequency.MINUTELY) {
            return Optional.of("TOO_HEAVY");
        }
        return Optional.empty();
    }

    /**
     * What expanding a rule {@link #refusal} accepts costs from {@code seed} over {@code [from, to)}, ical4j being asked
     * from {@code leadSeconds} before {@code from}; empty when no window makes it affordable: too many steps from its
     * seed for ical4j's skip, or a period giving more candidates than a whole budget.
     */
    static OptionalLong of(Recur<Temporal> recur, LocalDateTime seed, LocalDateTime from, LocalDateTime to, long leadSeconds) {
        long step = stepSeconds(recur.getFrequency()) * Math.max(1, recur.getInterval());
        long lead = leadSeconds / step + 1;
        long fromSeed = Math.max(0, ChronoUnit.SECONDS.between(seed, to)) / step + 2;
        if (fromSeed >= OVERFLOWING_STEPS) {
            return OptionalLong.empty();
        }
        long walked;
        if (recur.getCount() > 0) {
            // ical4j counts from the seed, but gives up after maxIncrementCount empty steps in a row, and each COUNT
            // candidate (generated, or too early to count) ends such a stretch: at most (COUNT + 1) stretches.
            long giveUp = Configurator.getIntProperty(Recur.KEY_MAX_INCREMENT_COUNT).orElse(1_000);
            walked = giveUp > 0 ? Math.min(fromSeed, ((long) recur.getCount() + 1) * (giveUp + 1)) : fromSeed;
        } else {
            walked = Math.max(0, ChronoUnit.SECONDS.between(from, to)) / step + 2;
        }
        long perStep = perStep(recur);
        if (perStep >= CAP) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Math.multiplyExact(Math.addExact(walked, lead), perStep));
        } catch (ArithmeticException overflow) {
            // A bill that wraps around would pass the budget, and lift it for every rule after.
            return OptionalLong.empty();
        }
    }

    /**
     * ical4j keeps duplicate entries of a BY list and expands each one again, so the cost of a list is its length, not
     * its distinct values: a list repeating an entry, or longer than the values it can hold, is no rule to expand.
     */
    static boolean distinctAndShort(Recur<Temporal> recur) {
        return distinct(recur.getSecondList(), 61) && distinct(recur.getMinuteList(), 60) && distinct(recur.getHourList(), 24)
            && distinct(recur.getDayList(), 7 * 106) && distinct(recur.getMonthDayList(), 62)
            && distinct(recur.getYearDayList(), 732) && distinct(recur.getWeekNoList(), 106)
            && distinct(recur.getMonthList(), 12) && distinct(recur.getSetPosList(), 732);
    }

    private static boolean distinct(List<?> list, int most) {
        return list.size() <= most && new HashSet<>(list).size() == list.size();
    }

    private static long stepSeconds(Frequency frequency) {
        return switch (frequency) {
            case SECONDLY -> 1;
            case MINUTELY -> 60;
            case HOURLY -> 3_600;
            case DAILY -> 86_400;
            case WEEKLY -> 604_800;
            case MONTHLY -> 28 * 86_400L;
            case YEARLY -> 365 * 86_400L;
        };
    }

    /** An upper bound of the candidates one period of the rule produces (its lists hold distinct entries: see {@code distinctAndShort}). */
    static long perStep(Recur<Temporal> recur) {
        Frequency frequency = recur.getFrequency();
        boolean byMonth = !recur.getMonthList().isEmpty();
        boolean byWeekNo = !recur.getWeekNoList().isEmpty();
        long days = 0;
        for (WeekDay day : recur.getDayList()) {
            days += day.getOffset() != 0 ? 1 : switch (frequency) {
                case YEARLY -> byMonth ? 5 : byWeekNo ? 1 : 53;
                case MONTHLY -> 5;
                default -> 1;
            };
        }
        long monthDays = (long) recur.getMonthDayList().size() * (frequency == Frequency.YEARLY && !byMonth ? 12 : 1);
        long product = 1;
        for (long part : new long[] {days, monthDays, (long) recur.getWeekNoList().size() * 7, recur.getMonthList().size(),
            recur.getYearDayList().size(), recur.getHourList().size(), recur.getMinuteList().size(), recur.getSecondList().size()}) {
            product = Math.min(CAP, product * Math.max(1, part));
        }
        return product;
    }
}
