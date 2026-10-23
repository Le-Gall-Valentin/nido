package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.EventScheduleValidator;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.DateListProperty;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.util.Configurator;
import net.fortuna.ical4j.transform.recurrence.Frequency;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The occurrences of an event of an untrusted file, expanded by ical4j under a budget. ical4j's own limits do not hold
 * against a hostile rule: {@code VEvent.calculateRecurrenceSet} expands the whole window (a rule of seconds never comes
 * back), its give-up counts only <em>consecutive</em> empty steps, and its skip-ahead doubles an {@code int} that
 * overflows to 0 and loops forever. So no rule reaches ical4j before its work — the periods it may walk times the
 * candidates one period may give — has been charged to the budget of the reading, and a rule it cannot afford (or
 * that nido cannot hold at all) is {@link Refused}. One instance serves one reading of one file.
 */
final class BoundedExpansion {

    /**
     * What one reading may ask of ical4j, in candidates generated (an upper bound, see {@code perStep}). Measured on the
     * development machine: a file of 300 rules each charging 2 million is read in about 0.3 s, one expanded and the
     * others refused. The worst shape that fits, {@code DAILY;BYHOUR=0..23;COUNT=10000000} from 1600 (billed 3.8 million),
     * took about 1.6 s and 540 MB, and the sparse HOURLY shapes about 3 s: a budget spent in full costs about 2 to 3 s and some hundreds of MB, once per import or
     * refresh, which the rate limits of imports and the spacing of refreshes make acceptable.
     */
    static final long BUDGET = 5_000_000;

    /**
     * ical4j doubles an {@code int} multiplier to skip from DTSTART to the window; it overflows past 2^30 and the skip
     * then never ends. A rule needing this many steps from DTSTART is refused well before.
     */
    private static final long OVERFLOWING_STEPS = 1L << 29;
    /** No count of candidates is taken past the budget: a larger one is refused, and no product of them can overflow. */
    private static final long CAP = BUDGET;

    /** A rule that cannot be expanded: {@code detail} is the reason the report gives (TOO_HEAVY or BAD_RULE). */
    static final class Refused extends RuntimeException {
        private final String detail;
        private final boolean spent;
        private final boolean ofTheRule;

        Refused(String detail) {
            this(detail, false, false);
        }

        private Refused(String detail, boolean spent, boolean ofTheRule) {
            super(detail, null, false, false);
            this.detail = detail;
            this.spent = spent;
            this.ofTheRule = ofTheRule;
        }

        /** A rule that no window makes affordable: nido cannot hold it, or ical4j cannot be kept to it. */
        private static Refused always(String detail) {
            return new Refused(detail, false, true);
        }

        /**
         * True when the rule is refused whatever the window is. The others depend on it — a bill grows with the window —
         * so a reader that remembers refusals must try them again on the next one.
         */
        boolean ofTheRule() {
            return ofTheRule;
        }

        String detail() {
            return detail;
        }

        /**
         * True when the rule would have fitted a whole budget and only what earlier work spent kept it out: it is not
         * heavy in itself, so a reader sharing one budget between several calendars must not remember it as such.
         */
        boolean budgetSpent() {
            return spent;
        }
    }

    private final ZoneId zone;
    private final long budget;
    private long spent;

    BoundedExpansion(ZoneId zone) {
        this(zone, BUDGET);
    }

    /** A reading with its own budget: a smaller one for what is read at every view rather than once per import. */
    BoundedExpansion(ZoneId zone, long budget) {
        this.zone = zone;
        this.budget = budget;
    }

    /** Whether a rule might be said by a nido series at all: no work is spent on the others to check it. */
    static boolean mayBeSeries(Recur<Temporal> recur) {
        return recur.getFrequency() != null && switch (recur.getFrequency()) {
            case DAILY, WEEKLY, MONTHLY, YEARLY -> true;
            default -> false;
        } && recur.getHourList().isEmpty() && recur.getMinuteList().isEmpty() && recur.getSecondList().isEmpty()
            && recur.getYearDayList().isEmpty() && recur.getWeekNoList().isEmpty();
    }

    /** The starts the EXDATEs take out, in the space's zone. */
    Set<LocalDateTime> excluded(VEvent event) {
        Set<LocalDateTime> excluded = new HashSet<>();
        for (ExDate<Temporal> exDate : event.<ExDate<Temporal>>getProperties(Property.EXDATE)) {
            exDate.getDates().forEach(date -> excluded.add(IcalSupport.toLocal(date, zone)));
        }
        return excluded;
    }

    /** Every RDATE of the event, wherever it falls. */
    List<LocalDateTime> rdates(VEvent event) {
        List<LocalDateTime> dates = new ArrayList<>();
        for (DateListProperty<Temporal> rdate : event.<DateListProperty<Temporal>>getProperties(Property.RDATE)) {
            if (rdate.getParameter(Parameter.VALUE).filter(Value.PERIOD::equals).isPresent()) {
                ((RDate<Temporal>) rdate).getPeriods().ifPresent(periods ->
                    periods.forEach(period -> dates.add(IcalSupport.toLocal(period.getStart(), zone))));
            } else {
                rdate.getDates().forEach(date -> dates.add(IcalSupport.toLocal(date, zone)));
            }
        }
        return dates;
    }

    /**
     * The first {@code cap} starts the RRULEs give in {@code [from, to)}, in order, minus the EXDATEs and the EXRULEs —
     * nothing of the RDATEs nor of DTSTART itself, which RFC 5545 counts but a rule may not fit.
     */
    List<LocalDateTime> ofRules(VEvent event, IcalTimes times, ZonedDateTime from, ZonedDateTime to, int cap) {
        return starts(event, times, from, to, cap, false, true, false);
    }

    /** The same with the EXDATEs ignored: where the rule itself begins (RFC 5545 counts an excluded occurrence in a COUNT). */
    List<LocalDateTime> ofRulesWithoutExdates(VEvent event, IcalTimes times, ZonedDateTime from, ZonedDateTime to, int cap) {
        return starts(event, times, from, to, cap, false, false, false);
    }

    /** The first {@code cap} occurrences of the event in {@code [from, to)}: what {@link #ofRules} gives, its RDATEs, and its DTSTART. */
    List<LocalDateTime> occurrences(VEvent event, IcalTimes times, ZonedDateTime from, ZonedDateTime to, int cap) {
        return starts(event, times, from, to, cap, true, true, false);
    }

    /**
     * The same, but also those that began before {@code from} and run into the window (by the event's duration): the lead
     * is applied once, here, and not again by the caller.
     */
    List<LocalDateTime> occurrencesOverlapping(VEvent event, IcalTimes times, ZonedDateTime from, ZonedDateTime to, int cap) {
        return starts(event, times, from, to, cap, true, true, true);
    }

    private List<LocalDateTime> starts(VEvent event, IcalTimes times, ZonedDateTime from, ZonedDateTime to, int cap,
                                       boolean withDates, boolean applyExdates, boolean overlapping) {
        Set<LocalDateTime> excluded = applyExdates ? excluded(event) : new HashSet<>();
        // The rules are asked for what the EXDATEs may take out besides what is kept. (An EXRULE may take out more: the
        // result is then shorter than the cap, never wrong.)
        int wanted = (int) Math.min(Integer.MAX_VALUE, (long) cap + excluded.size());
        Set<LocalDateTime> found = expand(event.<RRule<Temporal>>getProperties(Property.RRULE).stream().map(RRule::getRecur).toList(),
            event, times, from, to, wanted, overlapping);
        if (withDates) {
            found.addAll(rdates(event));
            found.add(times.start());
        }
        // EXRULE, which RFC 5545 deprecates: expanded under the same budget, then taken out.
        excluded.addAll(expand(event.<ExRule<Temporal>>getProperties(Property.EXRULE).stream().map(ExRule::getRecur).toList(),
            event, times, from, to, wanted, overlapping));
        LocalDateTime low = overlapping ? from.toLocalDateTime().minus(Duration.between(times.start(), times.end()))
            : from.toLocalDateTime();
        LocalDateTime high = to.toLocalDateTime();
        return found.stream().filter(start -> !start.isBefore(low) && start.isBefore(high))
            .filter(start -> !excluded.contains(start)).limit(cap).toList();
    }

    private Set<LocalDateTime> expand(List<Recur<Temporal>> rules, VEvent event, IcalTimes times, ZonedDateTime from,
                                      ZonedDateTime to, int wanted, boolean overlapping) {
        Set<LocalDateTime> found = new TreeSet<>();
        if (rules.isEmpty()) {
            return found;
        }
        Optional<DtStart<Temporal>> dtStart = event.getProperty(Property.DTSTART);
        Temporal seed = dtStart.orElseThrow().getDate();
        // An occurrence that started before the window may still run into it, and a date is read at the start of its
        // day: ical4j is asked from a little before, and what starts before the window is dropped by the caller.
        // A reader that wants what runs into the window asks as far back as the event lasts, a date included.
        Duration lasting = Duration.between(times.start(), times.end());
        boolean dateSeed = seed instanceof LocalDate;
        boolean longDate = dateSeed && overlapping && lasting.compareTo(Duration.ofDays(1)) > 0;
        TemporalAmount lead = dateSeed && !longDate ? java.time.Period.ofDays(1) : lasting;
        long leadSeconds = dateSeed && !longDate ? 86_400 : Math.max(0, lasting.toSeconds());
        Temporal asked = from.minus(lead);
        for (Recur<Temporal> recur : rules) {
            charge(recur, dateSeed, times.start(), from.toLocalDateTime(), to.toLocalDateTime(), leadSeconds);
            recur.getDatesAsStream(seed, asked, to, wanted).forEach(date -> found.add(IcalSupport.toLocal(date, zone)));
        }
        return found;
    }

    /** Refuses a rule nido cannot hold or ical4j cannot be kept to, and otherwise spends what expanding it may cost. */
    private void charge(Recur<Temporal> recur, boolean dateStart, LocalDateTime seed, LocalDateTime from, LocalDateTime to,
                        long leadSeconds) {
        Frequency frequency = recur.getFrequency();
        if (frequency == null || recur.getInterval() > EventScheduleValidator.MAX_INTERVAL || !distinctAndShort(recur)
            || dateStart && (frequency == Frequency.HOURLY || !recur.getHourList().isEmpty()
                || !recur.getMinuteList().isEmpty() || !recur.getSecondList().isEmpty())) {
            throw Refused.always("BAD_RULE");
        }
        if (frequency == Frequency.SECONDLY || frequency == Frequency.MINUTELY) {
            throw Refused.always("TOO_HEAVY");
        }
        long step = stepSeconds(frequency) * Math.max(1, recur.getInterval());
        long lead = leadSeconds / step + 1;
        long fromSeed = Math.max(0, ChronoUnit.SECONDS.between(seed, to)) / step + 2;
        if (fromSeed >= OVERFLOWING_STEPS) {
            throw Refused.always("TOO_HEAVY");
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
            throw Refused.always("TOO_HEAVY");
        }
        try {
            bill(Math.multiplyExact(Math.addExact(walked, lead), perStep));
        } catch (ArithmeticException overflow) {
            // A bill that wraps around would pass the budget, and lift it for every rule after.
            throw Refused.always("TOO_HEAVY");
        }
    }

    /** Spends {@code units} of the budget, or refuses when they are not left. The budget only ever shrinks. */
    void bill(long units) {
        if (units < 0 || units > budget) {
            throw new Refused("TOO_HEAVY");
        }
        if (units > budget - spent) {
            throw new Refused("TOO_HEAVY", true, false);
        }
        spent += units;
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
