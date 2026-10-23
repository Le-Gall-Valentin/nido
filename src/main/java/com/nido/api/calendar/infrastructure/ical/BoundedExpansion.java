package com.nido.api.calendar.infrastructure.ical;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.DateListProperty;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * candidates one period may give, {@link RuleCost} — has been charged to the budget of the reading, and a rule it
 * cannot afford (or that nido cannot hold at all) is {@link Refused}. One instance serves one reading of one file.
 */
final class BoundedExpansion {

    /**
     * What one reading may ask of ical4j, in candidates generated (an upper bound, see {@link RuleCost#perStep}). Measured on the
     * development machine: a file of 300 rules each charging 2 million is read in about 0.3 s, one expanded and the
     * others refused. The worst shape that fits, {@code DAILY;BYHOUR=0..23;COUNT=10000000} from 1600 (billed 3.8 million),
     * took about 1.6 s and 540 MB, and the sparse HOURLY shapes about 3 s: a budget spent in full costs about 2 to 3 s and some hundreds of MB, once per import or
     * refresh, which the rate limits of imports and the spacing of refreshes make acceptable.
     */
    static final long BUDGET = 5_000_000;


    /**
     * A rule that cannot be expanded: {@code detail} is the reason the report gives — BAD_RULE, or TOO_HEAVY for a rule
     * too costly in itself or beyond what is left of the budget of the reading.
     */
    static final class Refused extends RuntimeException {
        private final String detail;
        private final boolean ofTheRule;

        Refused(String detail) {
            this(detail, false);
        }

        private Refused(String detail, boolean ofTheRule) {
            super(detail, null, false, false);
            this.detail = detail;
            this.ofTheRule = ofTheRule;
        }

        /** A rule that no window makes affordable: nido cannot hold it, or ical4j cannot be kept to it. */
        private static Refused always(String detail) {
            return new Refused(detail, true);
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

    /** The starts the EXDATEs take out, in the space's zone: each one converted is billed, as a candidate is. */
    Set<LocalDateTime> excluded(VEvent event) {
        Set<LocalDateTime> excluded = new HashSet<>();
        for (ExDate<Temporal> exDate : event.<ExDate<Temporal>>getProperties(Property.EXDATE)) {
            bill(exDate.getDates().size());
            exDate.getDates().forEach(date -> excluded.add(IcalSupport.toLocal(date, zone)));
        }
        return excluded;
    }

    /** Every RDATE of the event, wherever it falls: each one converted is billed, as a candidate is. */
    List<LocalDateTime> rdates(VEvent event) {
        List<LocalDateTime> dates = new ArrayList<>();
        for (DateListProperty<Temporal> rdate : event.<DateListProperty<Temporal>>getProperties(Property.RDATE)) {
            if (rdate.getParameter(Parameter.VALUE).filter(Value.PERIOD::equals).isPresent()) {
                ((RDate<Temporal>) rdate).getPeriods().ifPresent(periods -> {
                    bill(periods.size());
                    periods.forEach(period -> dates.add(IcalSupport.toLocal(period.getStart(), zone)));
                });
            } else {
                bill(rdate.getDates().size());
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
        ZonedDateTime asked = from.minus(lead);
        // Bounds of the seed's own kind: ical4j compares a floating time or a date with zoned bounds in the JVM's zone
        // (TemporalComparator), not the space's, and loses what lies at either edge of the window.
        Temporal low = switch (seed) {
            case LocalDateTime floating -> asked.toLocalDateTime();
            case LocalDate date -> asked.toLocalDate();
            default -> asked;
        };
        Temporal high = switch (seed) {
            case LocalDateTime floating -> to.toLocalDateTime();
            case LocalDate date -> to.toLocalDate();
            default -> to;
        };
        for (Recur<Temporal> recur : rules) {
            charge(recur, dateSeed, times.start(), from.toLocalDateTime(), to.toLocalDateTime(), leadSeconds);
            recur.getDatesAsStream(seed, low, high, wanted).forEach(date -> found.add(IcalSupport.toLocal(date, zone)));
        }
        return found;
    }

    /** Refuses a rule nido cannot hold or ical4j cannot be kept to, and otherwise spends what expanding it may cost. */
    private void charge(Recur<Temporal> recur, boolean dateStart, LocalDateTime seed, LocalDateTime from, LocalDateTime to,
                        long leadSeconds) {
        Optional<String> refused = RuleCost.refusal(recur, dateStart);
        if (refused.isPresent()) {
            throw Refused.always(refused.get());
        }
        bill(RuleCost.of(recur, seed, from, to, leadSeconds).orElseThrow(() -> Refused.always("TOO_HEAVY")));
    }

    /** Spends {@code units} of the budget, or refuses when they are not left. The budget only ever shrinks. */
    void bill(long units) {
        if (units < 0 || units > budget - spent) {
            throw new Refused("TOO_HEAVY");
        }
        spent += units;
    }
}
