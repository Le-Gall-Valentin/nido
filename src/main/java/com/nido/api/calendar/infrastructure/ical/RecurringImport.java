package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.IcalRecurrence;
import com.nido.api.calendar.domain.model.ImportedEvent;
import com.nido.api.calendar.domain.model.ImportedOccurrence;
import com.nido.api.calendar.domain.model.ImportedSeries;
import com.nido.api.calendar.domain.model.RecurrenceRules;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.Weekdays;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DateListProperty;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;

import java.time.Duration;
import java.time.Instant;
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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * What a recurring event of a file becomes. nido suggests a series for its rule (RecurrenceRules) and keeps it only if
 * its projector gives exactly the starts ical4j expands, in the space's zone, over two years from the first: one test
 * for a rule nido cannot say, for a zone whose summer time does not move with the space's, and for a mistake of the
 * translation itself. Anything else is unfolded: its occurrences from a year ago to two years ahead, 500 at most.
 *
 * <p>A rule is read from an untrusted file, so ical4j's work is bounded here, not only its result: {@code tooHeavy}
 * refuses, before any expansion, a rule whose cost cannot be kept small (a COUNT to follow from far back at a rhythm of
 * seconds, a rule giving millions of instants a period), and every expansion asks ical4j for at most as many dates as
 * it will keep ({@code Recur.getDatesAsStream}) — {@code VEvent.calculateRecurrenceSet} has no such limit, and a
 * {@code FREQ=SECONDLY} rule never comes back from it.
 */
final class RecurringImport {

    record Result(ImportedSeries series, List<ImportedEvent> events, boolean unfolded, int unfoldedOccurrences, boolean partial) {}

    static final int VERIFIED_YEARS = 2;
    static final int MAX_UNFOLDED = 500;
    /** How far a COUNT is followed to find its last day. */
    private static final int COUNT_HORIZON_YEARS = 100;
    /** How many occurrences are asked of ical4j to check a series: far more than any rule nido can say gives in two years. */
    private static final int VERIFIED_CAP = 1_000;
    /** The most ical4j may be asked to generate (candidates and steps counted) for one rule. */
    private static final long MAX_WORK = 2_000_000;
    /** Steps taken on a rule with no COUNT before it gives up on an empty stretch (ical4j's own limit is 1 000 in a row). */
    private static final long STEPS_WITHOUT_COUNT = 1_000;
    private static final long MAX_FACTOR = 1L << 20;

    private final ZoneId zone;
    private final LocalDate today;
    private final Locale locale;

    RecurringImport(ZoneId zone, LocalDate today, Locale locale) {
        this.zone = zone;
        this.today = today;
        this.locale = locale;
    }

    Result translate(VEvent master, List<VEvent> overrides, IcalTexts texts, IcalTimes times) {
        ZonedDateTime from = times.start().atZone(zone);
        ZonedDateTime to = from.plusYears(VERIFIED_YEARS);
        Set<LocalDateTime> excluded = excluded(master);
        List<LocalDateTime> expanded = starts(master, times, from, to, VERIFIED_CAP);
        if (expanded.isEmpty()) {
            // A rule giving nothing at all: what is left is its first day (RFC 5545 counts it).
            return new Result(null, List.of(Ical4jImportReaderAdapter.event(texts, times)), false, 0, false);
        }
        LocalDateTime first = expanded.getFirst();
        Optional<RecurringEventSeries> series = asSeries(master, texts, times, first)
            .filter(candidate -> expanded.size() < VERIFIED_CAP && projected(candidate, from, to, excluded).equals(expanded));
        return series.isPresent()
            ? asResult(series.get(), overrides, texts, times, first, excluded)
            : unfold(master, overrides, texts, times);
    }

    private Optional<RecurringEventSeries> asSeries(VEvent master, IcalTexts texts, IcalTimes times, LocalDateTime first) {
        Optional<RRule<Temporal>> rrule = master.getProperty(Property.RRULE);
        if (rrule.isEmpty()) {
            return Optional.empty();
        }
        Recur<Temporal> recur = rrule.get().getRecur();
        IcalTimes occurrence = times.at(first);
        return RecurrenceRules.candidateFor(recurrence(recur), first.toLocalDate()).flatMap(candidate -> {
            try {
                int durationDays = (int) ChronoUnit.DAYS.between(occurrence.startDate(), occurrence.endDate());
                LocalDate endDate = endDate(master, recur, occurrence);
                EventScheduleValidator.validateSeries(occurrence.allDay(), occurrence.startTime(), occurrence.endTime(),
                    durationDays, candidate.intervalType(), candidate.intervalCount(), candidate.weekdays(),
                    candidate.dayRule(), candidate.anchorDate(), endDate);
                return Optional.of(new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), texts.title(),
                    texts.description(), texts.location(), occurrence.allDay(), occurrence.startTime(), occurrence.endTime(),
                    durationDays, null, candidate.intervalType(), candidate.intervalCount(), candidate.weekdays(),
                    candidate.dayRule(), candidate.anchorDate(), endDate, candidate.startsOn(), List.of(),
                    UUID.randomUUID(), Instant.EPOCH));
            } catch (CalendarException | IllegalArgumentException refused) {
                return Optional.empty();
            }
        });
    }

    /** The rule's parts nido may say, out of ical4j's own reading of it. */
    private static IcalRecurrence recurrence(Recur<Temporal> recur) {
        boolean otherParts = !recur.getHourList().isEmpty() || !recur.getMinuteList().isEmpty()
            || !recur.getSecondList().isEmpty() || !recur.getYearDayList().isEmpty() || !recur.getWeekNoList().isEmpty();
        return new IcalRecurrence(recur.getFrequency().name(), Math.max(1, recur.getInterval()),
            recur.getDayList().stream()
                .map(day -> new IcalRecurrence.ByDay(day.getOffset(), Weekdays.fromCode(day.getDay().name()))).toList(),
            recur.getMonthDayList(), recur.getMonthList().stream().map(month -> month.getMonthOfYear()).toList(),
            recur.getSetPosList(), otherParts);
    }

    /** The last day an occurrence may start on: the last one a COUNT gives, or UNTIL's day — the day before when its
     * time falls before the occurrence's. */
    private LocalDate endDate(VEvent master, Recur<Temporal> recur, IcalTimes occurrence) {
        if (recur.getCount() > 0) {
            ZonedDateTime from = occurrence.start().atZone(zone);
            List<LocalDateTime> all = starts(master, occurrence, from, from.plusYears(COUNT_HORIZON_YEARS), Integer.MAX_VALUE);
            return all.isEmpty() ? null : all.getLast().toLocalDate();
        }
        if (recur.getUntil() != null) {
            LocalDateTime until = IcalSupport.toLocal(recur.getUntil(), zone);
            boolean beforeTheOccurrence = !occurrence.allDay() && until.toLocalTime().isBefore(occurrence.startTime());
            return beforeTheOccurrence ? until.toLocalDate().minusDays(1) : until.toLocalDate();
        }
        return null;
    }

    private static List<LocalDateTime> projected(RecurringEventSeries series, ZonedDateTime from, ZonedDateTime to,
                                                 Set<LocalDateTime> excluded) {
        LocalDateTime start = from.toLocalDateTime();
        LocalDateTime end = to.toLocalDateTime();
        return EventRecurrenceProjector.slotsBetween(series, from.toLocalDate(), to.toLocalDate()).stream()
            .map(day -> series.allDay() ? day.atStartOfDay() : day.atTime(series.startTime()))
            .filter(at -> !at.isBefore(start) && at.isBefore(end))
            .filter(at -> !excluded.contains(at))
            .toList();
    }

    private Result asResult(RecurringEventSeries series, List<VEvent> overrides, IcalTexts texts, IcalTimes times,
                            LocalDateTime first, Set<LocalDateTime> excluded) {
        List<ImportedEvent> ownEvents = new ArrayList<>();
        if (!first.equals(times.start()) && !excluded.contains(times.start())) {
            ownEvents.add(Ical4jImportReaderAdapter.event(texts, times));
        }
        Set<LocalDate> cancelled = new TreeSet<>();
        excluded.stream().map(LocalDateTime::toLocalDate).filter(day -> isSlot(series, day)).forEach(cancelled::add);
        List<ImportedOccurrence> edited = new ArrayList<>();
        for (VEvent override : overrides) {
            Optional<LocalDateTime> replaced = replaced(override);
            if (replaced.isEmpty()) {
                continue;
            }
            LocalDate slot = replaced.get().toLocalDate();
            boolean isCancelled = Ical4jImportReaderAdapter.cancelled(override);
            Optional<ImportedEvent> event = IcalTimes.of(override, zone)
                .map(t -> Ical4jImportReaderAdapter.event(IcalTexts.of(override, locale), t));
            if (isSlot(series, slot)) {
                if (isCancelled) {
                    cancelled.add(slot);
                } else {
                    event.ifPresent(e -> edited.add(new ImportedOccurrence(slot, e)));
                }
            } else if (!isCancelled) {
                event.ifPresent(ownEvents::add);
            }
        }
        return new Result(new ImportedSeries(series.title(), series.description(), series.location(), series.allDay(),
            series.startTime(), series.endTime(), series.durationDays(), series.intervalType(), series.intervalCount(),
            series.weekdays(), series.dayRule(), series.anchorDate(), series.startsOn(), series.endDate(), cancelled, edited),
            ownEvents, false, 0, false);
    }

    private Result unfold(VEvent master, List<VEvent> overrides, IcalTexts texts, IcalTimes times) {
        ZonedDateTime from = today.minusYears(1).atStartOfDay(zone);
        ZonedDateTime to = today.plusYears(VERIFIED_YEARS).plusDays(1).atStartOfDay(zone);
        Set<LocalDateTime> replaced = overrides.stream().map(this::replaced).flatMap(Optional::stream).collect(Collectors.toSet());
        // Asked for one more than is kept, to know whether some were left out, and for as many as the overrides may take out.
        List<LocalDateTime> all = starts(master, times, from, to, MAX_UNFOLDED + 1 + replaced.size()).stream()
            .filter(start -> !replaced.contains(start))
            .toList();
        List<LocalDateTime> starts = all.stream().limit(MAX_UNFOLDED).toList();
        List<ImportedEvent> events = new ArrayList<>();
        starts.forEach(start -> events.add(Ical4jImportReaderAdapter.event(texts, times.at(start))));
        for (VEvent override : overrides) {
            if (!Ical4jImportReaderAdapter.cancelled(override)) {
                IcalTimes.of(override, zone)
                    .filter(t -> !t.start().isBefore(from.toLocalDateTime()) && t.start().isBefore(to.toLocalDateTime()))
                    .ifPresent(t -> events.add(Ical4jImportReaderAdapter.event(IcalTexts.of(override, locale), t)));
            }
        }
        boolean partial = all.size() > MAX_UNFOLDED || times.start().isBefore(from.toLocalDateTime())
            || endless(master) || !starts(master, times, to, to.plusYears(50), 1).isEmpty();
        return new Result(null, events, true, starts.size(), partial);
    }

    private static boolean endless(VEvent master) {
        Optional<RRule<Temporal>> rrule = master.getProperty(Property.RRULE);
        return rrule.isPresent() && rrule.get().getRecur().getCount() < 1 && rrule.get().getRecur().getUntil() == null;
    }

    private static boolean isSlot(RecurringEventSeries series, LocalDate day) {
        return EventRecurrenceProjector.slotsBetween(series, day, day).contains(day);
    }

    /** The slot an edited occurrence of the file takes over, in the space's zone. */
    private Optional<LocalDateTime> replaced(VEvent override) {
        return replaced(override, zone);
    }

    /** The slot an edited occurrence of the file takes over, in {@code zone}. */
    static Optional<LocalDateTime> replaced(VEvent override, ZoneId zone) {
        Optional<RecurrenceId<Temporal>> id = override.getProperty(Property.RECURRENCE_ID);
        return id.map(RecurrenceId::getDate).map(date -> IcalSupport.toLocal(date, zone));
    }

    private Set<LocalDateTime> excluded(VEvent master) {
        Set<LocalDateTime> excluded = new HashSet<>();
        for (ExDate<Temporal> exDate : master.<ExDate<Temporal>>getProperties(Property.EXDATE)) {
            exDate.getDates().forEach(date -> excluded.add(IcalSupport.toLocal(date, zone)));
        }
        return excluded;
    }

    /**
     * Whether ical4j cannot be kept to a small amount of work on this event's rules: what it would generate — candidates
     * of each period, times the periods followed (all of them up to the last day any step reads, when a COUNT forces
     * it to count from the first) — is past {@code MAX_WORK}. Such an event cannot be read at all.
     */
    boolean tooHeavy(VEvent master, IcalTimes times) {
        LocalDateTime horizon = LocalDateTime.of(Math.max(times.start().getYear() + COUNT_HORIZON_YEARS,
            today.getYear() + VERIFIED_YEARS + 50), 12, 31, 0, 0);
        for (RRule<Temporal> rule : master.<RRule<Temporal>>getProperties(Property.RRULE)) {
            Recur<Temporal> recur = rule.getRecur();
            long factor = 1;
            for (int size : new int[] {recur.getDayList().size(), recur.getMonthDayList().size(), recur.getMonthList().size(),
                recur.getHourList().size(), recur.getMinuteList().size(), recur.getSecondList().size(),
                recur.getYearDayList().size(), recur.getWeekNoList().size()}) {
                factor = Math.min(MAX_FACTOR, factor * Math.max(1, size));
            }
            long steps = STEPS_WITHOUT_COUNT;
            if (recur.getCount() > 0) {
                long reached = Math.max(0, ChronoUnit.SECONDS.between(times.start(), horizon)) / secondsOf(recur);
                steps = Math.min(recur.getCount(), reached + 1);
            }
            if (factor * steps > MAX_WORK) {
                return true;
            }
        }
        return false;
    }

    /** How long one step of the rule lasts, near enough to count how many it takes to reach a day. */
    private static long secondsOf(Recur<Temporal> recur) {
        long unit = recur.getFrequency() == null ? 1 : switch (recur.getFrequency()) {
            case SECONDLY -> 1;
            case MINUTELY -> 60;
            case HOURLY -> 3_600;
            case DAILY -> 86_400;
            case WEEKLY -> 604_800;
            case MONTHLY -> 2_629_746;
            case YEARLY -> 31_556_952;
        };
        return unit * Math.max(1, recur.getInterval());
    }

    /**
     * The first {@code cap} local starts of the event in {@code [from, to)}, in order — EXDATE applied, edited
     * occurrences not taken out. Not {@code calculateRecurrenceSet}: it expands the whole window, so that a rule of
     * seconds never returns. Here ical4j is asked for {@code cap} dates (and as many as the EXDATEs may remove) and
     * stops there. EXRULE, which RFC 5545 deprecates, is not applied.
     */
    private List<LocalDateTime> starts(VEvent event, IcalTimes times, ZonedDateTime from, ZonedDateTime to, int cap) {
        Optional<DtStart<Temporal>> dtStart = event.getProperty(Property.DTSTART);
        Temporal seed = dtStart.orElseThrow().getDate();
        Set<LocalDateTime> excluded = excluded(event);
        LocalDateTime low = from.toLocalDateTime();
        LocalDateTime high = to.toLocalDateTime();
        // An occurrence that started before the window may still run into it, and a date is read at the start of its day:
        // ical4j is asked from a little before, and what starts before the window is dropped here.
        TemporalAmount lead = seed instanceof LocalDate ? java.time.Period.ofDays(1) : Duration.between(times.start(), times.end());
        Temporal asked = from.minus(lead);
        int wanted = (int) Math.min(Integer.MAX_VALUE, (long) cap + excluded.size());
        Set<LocalDateTime> found = new TreeSet<>();
        List<RRule<Temporal>> rules = event.getProperties(Property.RRULE);
        for (RRule<Temporal> rule : rules) {
            rule.getRecur().getDatesAsStream(seed, asked, to, wanted).forEach(date -> found.add(IcalSupport.toLocal(date, zone)));
        }
        if (rules.isEmpty()) {
            found.add(IcalSupport.toLocal(seed, zone));
        }
        for (DateListProperty<Temporal> rdate : event.<DateListProperty<Temporal>>getProperties(Property.RDATE)) {
            if (rdate.getParameter(Parameter.VALUE).filter(Value.PERIOD::equals).isPresent()) {
                ((RDate<Temporal>) rdate).getPeriods().ifPresent(periods ->
                    periods.forEach(period -> found.add(IcalSupport.toLocal(period.getStart(), zone))));
            } else {
                rdate.getDates().forEach(date -> found.add(IcalSupport.toLocal(date, zone)));
            }
        }
        return found.stream().filter(start -> !start.isBefore(low) && start.isBefore(high))
            .filter(start -> !excluded.contains(start)).limit(cap).toList();
    }
}
