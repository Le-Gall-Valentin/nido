package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.RecurrenceRules;
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
     * development machine: a file of 300 rules, each charging 2 million, is read in about 0.3 s, one of them expanded
     * and the others refused; the budget spent in full is therefore well under a second.
     */
    static final long BUDGET = 5_000_000;

    /**
     * ical4j doubles an {@code int} multiplier to skip from DTSTART to the window; it overflows past 2^30 and the skip
     * then never ends. A rule needing this many steps from DTSTART is refused well before.
     */
    private static final long OVERFLOWING_STEPS = 1L << 29;
    private static final long CAP = 1L << 40;

    /** A rule that cannot be expanded: {@code detail} is the reason the report gives (TOO_HEAVY or BAD_RULE). */
    static final class Refused extends RuntimeException {
        private final String detail;

        Refused(String detail) {
            super(detail, null, false, false);
            this.detail = detail;
        }

        String detail() {
            return detail;
        }
    }

    private final ZoneId zone;
    private long spent;

    BoundedExpansion(ZoneId zone) {
        this.zone = zone;
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
        return starts(event, times, from, to, cap, false);
    }

    /** The first {@code cap} occurrences of the event in {@code [from, to)}: what {@link #ofRules} gives, its RDATEs, and its DTSTART. */
    List<LocalDateTime> occurrences(VEvent event, IcalTimes times, ZonedDateTime from, ZonedDateTime to, int cap) {
        return starts(event, times, from, to, cap, true);
    }

    private List<LocalDateTime> starts(VEvent event, IcalTimes times, ZonedDateTime from, ZonedDateTime to, int cap,
                                       boolean withDates) {
        Set<LocalDateTime> excluded = excluded(event);
        // The rules are asked for what the EXDATEs may take out besides what is kept. (An EXRULE may take out more: the
        // result is then shorter than the cap, never wrong.)
        int wanted = (int) Math.min(Integer.MAX_VALUE, (long) cap + excluded.size());
        Set<LocalDateTime> found = expand(event.<RRule<Temporal>>getProperties(Property.RRULE).stream().map(RRule::getRecur).toList(),
            event, times, from, to, wanted);
        if (withDates) {
            found.addAll(rdates(event));
            found.add(times.start());
        }
        // EXRULE, which RFC 5545 deprecates: expanded under the same budget, then taken out.
        excluded.addAll(expand(event.<ExRule<Temporal>>getProperties(Property.EXRULE).stream().map(ExRule::getRecur).toList(),
            event, times, from, to, wanted));
        LocalDateTime low = from.toLocalDateTime();
        LocalDateTime high = to.toLocalDateTime();
        return found.stream().filter(start -> !start.isBefore(low) && start.isBefore(high))
            .filter(start -> !excluded.contains(start)).limit(cap).toList();
    }

    private Set<LocalDateTime> expand(List<Recur<Temporal>> rules, VEvent event, IcalTimes times, ZonedDateTime from,
                                      ZonedDateTime to, int wanted) {
        Set<LocalDateTime> found = new TreeSet<>();
        if (rules.isEmpty()) {
            return found;
        }
        Optional<DtStart<Temporal>> dtStart = event.getProperty(Property.DTSTART);
        Temporal seed = dtStart.orElseThrow().getDate();
        // An occurrence that started before the window may still run into it, and a date is read at the start of its
        // day: ical4j is asked from a little before, and what starts before the window is dropped by the caller.
        TemporalAmount lead = seed instanceof LocalDate ? java.time.Period.ofDays(1) : Duration.between(times.start(), times.end());
        Temporal asked = from.minus(lead);
        for (Recur<Temporal> recur : rules) {
            charge(recur, seed instanceof LocalDate, times.start(), from.toLocalDateTime(), to.toLocalDateTime());
            recur.getDatesAsStream(seed, asked, to, wanted).forEach(date -> found.add(IcalSupport.toLocal(date, zone)));
        }
        return found;
    }

    /** Refuses a rule nido cannot hold or ical4j cannot be kept to, and otherwise spends what expanding it may cost. */
    private void charge(Recur<Temporal> recur, boolean dateStart, LocalDateTime seed, LocalDateTime from, LocalDateTime to) {
        Frequency frequency = recur.getFrequency();
        if (frequency == null || recur.getInterval() > RecurrenceRules.MAX_INTERVAL
            || dateStart && (frequency == Frequency.HOURLY || !recur.getHourList().isEmpty()
                || !recur.getMinuteList().isEmpty() || !recur.getSecondList().isEmpty())) {
            throw new Refused("BAD_RULE");
        }
        if (frequency == Frequency.SECONDLY || frequency == Frequency.MINUTELY) {
            throw new Refused("TOO_HEAVY");
        }
        long step = stepSeconds(frequency) * Math.max(1, recur.getInterval());
        long fromSeed = Math.max(0, ChronoUnit.SECONDS.between(seed, to)) / step + 2;
        if (fromSeed >= OVERFLOWING_STEPS) {
            throw new Refused("TOO_HEAVY");
        }
        // With a COUNT, ical4j counts from the seed; without, it skips ahead to the window.
        long walked = recur.getCount() > 0 ? fromSeed : Math.max(0, ChronoUnit.SECONDS.between(from, to)) / step + 2;
        long work = walked * perStep(recur);
        if (work > BUDGET - spent) {
            throw new Refused("TOO_HEAVY");
        }
        spent += work;
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

    /** An upper bound of the candidates one period of the rule produces. */
    private static long perStep(Recur<Temporal> recur) {
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
        long instants = switch (frequency) {
            case YEARLY -> 366;
            case MONTHLY -> 31;
            case WEEKLY -> 7;
            default -> 1;
        };
        instants *= (recur.getHourList().isEmpty() ? 1 : 24) * (recur.getMinuteList().isEmpty() ? 1 : 60)
            * (recur.getSecondList().isEmpty() ? 1 : 60);
        return Math.min(product, instants);
    }
}
