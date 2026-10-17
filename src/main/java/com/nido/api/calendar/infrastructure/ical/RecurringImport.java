package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.IcalRecurrence;
import com.nido.api.calendar.domain.model.ImportNote;
import com.nido.api.calendar.domain.model.ImportedEvent;
import com.nido.api.calendar.domain.model.ImportedOccurrence;
import com.nido.api.calendar.domain.model.ImportedSeries;
import com.nido.api.calendar.domain.model.RecurrenceRules;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.Weekdays;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.zone.ZoneOffsetTransition;
import java.util.ArrayList;
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
 * Every expansion goes through the reading's {@link BoundedExpansion}, which refuses what it cannot afford.
 */
final class RecurringImport {

    record Result(ImportedSeries series, List<ImportedEvent> events, boolean unfolded, int unfoldedOccurrences, boolean partial) {}

    static final int VERIFIED_YEARS = 2;
    static final int MAX_UNFOLDED = 500;
    /** How far a COUNT is followed to find its last day. */
    private static final int COUNT_HORIZON_YEARS = 100;
    /** How long an open-ended series is checked to keep its hours when its zone is not the space's. */
    private static final int OFFSET_YEARS = 10;
    /** How many occurrences are asked of ical4j to check a series: far more than any rule nido can say gives in two years. */
    private static final int VERIFIED_CAP = 1_000;

    private final ZoneId zone;
    private final LocalDate today;
    private final Locale locale;
    private final BoundedExpansion expansion;

    RecurringImport(ZoneId zone, LocalDate today, Locale locale, BoundedExpansion expansion) {
        this.zone = zone;
        this.today = today;
        this.locale = locale;
        this.expansion = expansion;
    }

    /** May throw {@link BoundedExpansion.Refused}, or whatever ical4j throws on a rule it cannot expand: the caller isolates the group. */
    Result translate(VEvent master, List<VEvent> overrides, IcalTexts texts, IcalTimes times, List<ImportNote> notes) {
        List<RRule<Temporal>> rules = master.getProperties(Property.RRULE);
        boolean oneRule = rules.size() == 1 && master.getProperties(Property.EXRULE).isEmpty();
        if (oneRule && BoundedExpansion.mayBeSeries(rules.getFirst().getRecur())) {
            ZonedDateTime from = times.start().atZone(zone);
            ZonedDateTime to = from.plusYears(VERIFIED_YEARS);
            Set<LocalDateTime> excluded = expansion.excluded(master);
            List<LocalDateTime> expanded = expansion.ofRules(master, times, from, to, VERIFIED_CAP);
            if (expanded.isEmpty() && expansion.rdates(master).isEmpty()) {
                // A rule giving nothing at all: what is left is its first day (RFC 5545 counts it), unless it is excluded.
                List<ImportedEvent> first = excluded.contains(times.start()) ? List.of()
                    : List.of(Ical4jImportReaderAdapter.event(texts, times));
                return new Result(null, first, false, 0, false);
            }
            if (!expanded.isEmpty()) {
                LocalDateTime first = expanded.getFirst();
                Optional<RecurringEventSeries> series = asSeries(master, texts, times, first)
                    .filter(candidate -> expanded.size() < VERIFIED_CAP
                        && projected(candidate, from, to, excluded).equals(expanded)
                        && expansion.rdates(master).stream().allMatch(at -> excluded.contains(at) || isSlotStart(candidate, at))
                        && offsetsStay(master, candidate));
                if (series.isPresent()) {
                    return asResult(series.get(), overrides, texts, times, first, excluded, notes);
                }
            }
        }
        return unfold(master, overrides, texts, times, notes);
    }

    private Optional<RecurringEventSeries> asSeries(VEvent master, IcalTexts texts, IcalTimes times, LocalDateTime first) {
        Recur<Temporal> recur = master.<RRule<Temporal>>getProperty(Property.RRULE).orElseThrow().getRecur();
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

    /** The last day an occurrence may start on: the last one a COUNT gives (by the rule alone: an RDATE is not its end), or
     * UNTIL's day — the day before when its time falls before the occurrence's. */
    private LocalDate endDate(VEvent master, Recur<Temporal> recur, IcalTimes occurrence) {
        if (recur.getCount() > 0) {
            ZonedDateTime from = occurrence.start().atZone(zone);
            List<LocalDateTime> all = expansion.ofRules(master, occurrence, from, from.plusYears(COUNT_HORIZON_YEARS), Integer.MAX_VALUE);
            return all.isEmpty() ? null : all.getLast().toLocalDate();
        }
        if (recur.getUntil() != null) {
            LocalDateTime until = IcalSupport.toLocal(recur.getUntil(), zone);
            boolean beforeTheOccurrence = !occurrence.allDay() && until.toLocalTime().isBefore(occurrence.startTime());
            return beforeTheOccurrence ? until.toLocalDate().minusDays(1) : until.toLocalDate();
        }
        return null;
    }

    /**
     * A timed event written in another zone keeps its hours in the space's only while the two zones' offsets keep the same
     * gap — the weeks when the United States and Europe disagree on summer time break it, and no check window of two years
     * is sure to meet them. Walked on both zones' transitions from DTSTART to the series' end, or ten years on.
     */
    private boolean offsetsStay(VEvent master, RecurringEventSeries series) {
        Temporal start = master.<DtStart<Temporal>>getProperty(Property.DTSTART).orElseThrow().getDate();
        ZoneId source = switch (start) {
            case ZonedDateTime zoned -> zoned.getZone();
            case OffsetDateTime offset -> offset.getOffset();
            case Instant instant -> ZoneOffset.UTC;
            default -> null;
        };
        if (series.allDay() || source == null || source.getRules().equals(zone.getRules())) {
            return true;
        }
        Instant first = IcalSupport.toLocal(start, zone).atZone(zone).toInstant();
        Instant last = (series.endDate() == null ? first.atZone(zone).plusYears(OFFSET_YEARS)
            : series.endDate().plusDays(1).atStartOfDay(zone)).toInstant();
        int gap = gap(source, first);
        Instant at = first;
        while (true) {
            ZoneOffsetTransition mine = zone.getRules().nextTransition(at);
            ZoneOffsetTransition theirs = source.getRules().nextTransition(at);
            Instant next = mine == null ? (theirs == null ? null : theirs.getInstant())
                : theirs == null || mine.getInstant().isBefore(theirs.getInstant()) ? mine.getInstant() : theirs.getInstant();
            if (next == null || next.isAfter(last)) {
                return true;
            }
            if (gap(source, next) != gap) {
                return false;
            }
            at = next;
        }
    }

    private int gap(ZoneId source, Instant at) {
        return source.getRules().getOffset(at).getTotalSeconds() - zone.getRules().getOffset(at).getTotalSeconds();
    }

    private static List<LocalDateTime> projected(RecurringEventSeries series, ZonedDateTime from, ZonedDateTime to,
                                                 Set<LocalDateTime> excluded) {
        LocalDateTime start = from.toLocalDateTime();
        LocalDateTime end = to.toLocalDateTime();
        return EventRecurrenceProjector.slotsBetween(series, from.toLocalDate(), to.toLocalDate()).stream()
            .map(day -> startOf(series, day))
            .filter(at -> !at.isBefore(start) && at.isBefore(end))
            .filter(at -> !excluded.contains(at))
            .toList();
    }

    private static LocalDateTime startOf(RecurringEventSeries series, LocalDate day) {
        return series.allDay() ? day.atStartOfDay() : day.atTime(series.startTime());
    }

    /** Whether a series has an occurrence starting exactly then: a slot of its days, at its time. */
    private static boolean isSlotStart(RecurringEventSeries series, LocalDateTime at) {
        return isSlot(series, at.toLocalDate()) && startOf(series, at.toLocalDate()).equals(at);
    }

    private static boolean isSlot(RecurringEventSeries series, LocalDate day) {
        return EventRecurrenceProjector.slotsBetween(series, day, day).contains(day);
    }

    private Result asResult(RecurringEventSeries series, List<VEvent> overrides, IcalTexts texts, IcalTimes times,
                            LocalDateTime first, Set<LocalDateTime> excluded, List<ImportNote> notes) {
        List<ImportedEvent> ownEvents = new ArrayList<>();
        if (!first.equals(times.start()) && !excluded.contains(times.start())) {
            ownEvents.add(Ical4jImportReaderAdapter.event(texts, times));
        }
        Set<LocalDate> cancelled = new TreeSet<>();
        // An EXDATE frees a slot only when it names the slot's own start.
        excluded.stream().filter(at -> isSlotStart(series, at)).map(LocalDateTime::toLocalDate).forEach(cancelled::add);
        List<ImportedOccurrence> edited = new ArrayList<>();
        for (VEvent override : overrides) {
            Optional<LocalDateTime> replaced = replaced(override, zone);
            if (replaced.isEmpty()) {
                continue;
            }
            LocalDate slot = replaced.get().toLocalDate();
            boolean isCancelled = Ical4jImportReaderAdapter.cancelled(override);
            Optional<ImportedEvent> event = eventOf(override, notes, !isCancelled);
            if (isSlotStart(series, replaced.get())) {
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

    /** The event an override says, with the usual notes of its texts and times when {@code noted}. */
    private Optional<ImportedEvent> eventOf(VEvent override, List<ImportNote> notes, boolean noted) {
        return IcalTimes.of(override, zone).map(t -> {
            IcalTexts texts = IcalTexts.of(override, locale);
            if (noted) {
                Ical4jImportReaderAdapter.notesOf(texts, t, notes);
            }
            return Ical4jImportReaderAdapter.event(texts, t);
        });
    }

    private Result unfold(VEvent master, List<VEvent> overrides, IcalTexts texts, IcalTimes times, List<ImportNote> notes) {
        ZonedDateTime from = today.minusYears(1).atStartOfDay(zone);
        ZonedDateTime to = today.plusYears(VERIFIED_YEARS).plusDays(1).atStartOfDay(zone);
        Set<LocalDateTime> replaced = overrides.stream().map(o -> replaced(o, zone)).flatMap(Optional::stream).collect(Collectors.toSet());
        // Asked for one more than is kept, to know whether some were left out, and for as many as the overrides may take out.
        List<LocalDateTime> all = expansion.occurrences(master, times, from, to, MAX_UNFOLDED + 1 + replaced.size()).stream()
            .filter(start -> !replaced.contains(start))
            .toList();
        List<LocalDateTime> starts = all.stream().limit(MAX_UNFOLDED).toList();
        List<ImportedEvent> events = new ArrayList<>();
        starts.forEach(start -> events.add(Ical4jImportReaderAdapter.event(texts, times.at(start))));
        for (VEvent override : overrides) {
            if (!Ical4jImportReaderAdapter.cancelled(override)) {
                IcalTimes.of(override, zone)
                    .filter(t -> !t.start().isBefore(from.toLocalDateTime()) && t.start().isBefore(to.toLocalDateTime()))
                    .flatMap(t -> eventOf(override, notes, true))
                    .ifPresent(events::add);
            }
        }
        boolean partial = all.size() > MAX_UNFOLDED || times.start().isBefore(from.toLocalDateTime())
            || endless(master) || !expansion.occurrences(master, times, to, to.plusYears(50), 1).isEmpty();
        return new Result(null, events, true, starts.size(), partial);
    }

    private static boolean endless(VEvent master) {
        return master.<RRule<Temporal>>getProperties(Property.RRULE).stream()
            .anyMatch(rule -> rule.getRecur().getCount() < 1 && rule.getRecur().getUntil() == null);
    }

    /** The slot an edited occurrence of the file takes over, in {@code zone}. */
    static Optional<LocalDateTime> replaced(VEvent override, ZoneId zone) {
        Optional<RecurrenceId<Temporal>> id = override.getProperty(Property.RECURRENCE_ID);
        return id.map(RecurrenceId::getDate).map(date -> IcalSupport.toLocal(date, zone));
    }
}
