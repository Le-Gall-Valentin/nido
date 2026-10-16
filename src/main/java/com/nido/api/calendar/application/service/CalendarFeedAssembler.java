package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarFeed;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.EventRecurrenceProjector;
import com.nido.api.calendar.domain.model.FeedSeries;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Gathers what an export or a subscription writes, as one member of the space reads it.
 *
 * <p>The space's own events go in full: a series is a rule, and an iCalendar reader expands it itself. The other sources
 * have no rule to hand over — a recurring task or debit is a projection, sometimes endless —, so they go as single
 * events from a season back to a year ahead, read through the same sources as the calendar, in windows no longer than
 * one calendar read.
 *
 * <p>"Only what concerns me" keeps what the reader takes part in, and what nobody in particular does: an event with no
 * participant, a task assigned to nobody. Meals, money and savings have no addressee, and all stay.
 */
@ApplicationService
public class CalendarFeedAssembler {

    static final int MONTHS_BACK = 3;
    static final int MONTHS_AHEAD = 12;

    private final CalendarEventRepository events;
    private final RecurringEventSeriesRepository seriesRepository;
    private final EventExclusionRepository exclusions;
    private final List<CalendarSource> sources;
    private final GetSpaceUseCase spaces;
    private final GetSpaceTodayUseCase spaceToday;

    public CalendarFeedAssembler(CalendarEventRepository events, RecurringEventSeriesRepository seriesRepository,
                                 EventExclusionRepository exclusions, List<CalendarSource> sources,
                                 GetSpaceUseCase spaces, GetSpaceTodayUseCase spaceToday) {
        this.events = events;
        this.seriesRepository = seriesRepository;
        this.exclusions = exclusions;
        this.sources = sources;
        this.spaces = spaces;
        this.spaceToday = spaceToday;
    }

    public CalendarFeed assemble(SpaceMembership reader, Set<CalendarSourceType> wanted, boolean onlyMine) {
        SpaceDetailView space = spaces.get(reader.spaceId(), reader);
        Predicate<List<UUID>> concerns = people -> !onlyMine || people.isEmpty() || people.contains(reader.userId());
        List<CalendarEvent> plain = new ArrayList<>();
        List<FeedSeries> written = new ArrayList<>();
        if (wanted.contains(CalendarSourceType.EVENT)) {
            ownEvents(reader, concerns, plain, written);
        }
        return new CalendarFeed(space.name(), space.timezone(), plain, written, others(reader, wanted, concerns));
    }

    private void ownEvents(SpaceMembership reader, Predicate<List<UUID>> concerns,
                           List<CalendarEvent> plain, List<FeedSeries> written) {
        Map<UUID, List<CalendarEvent>> editedBySeries = new HashMap<>();
        for (CalendarEvent event : events.findBySpaceId(reader.spaceId())) {
            if (event.isDetachedOccurrence()) {
                editedBySeries.computeIfAbsent(event.recurringSeriesId(), id -> new ArrayList<>()).add(event);
            } else if (concerns.test(event.participantIds())) {
                plain.add(event);
            }
        }
        List<RecurringEventSeries> all = seriesRepository.findBySpaceId(reader.spaceId());
        Map<UUID, Set<LocalDate>> cancelled = exclusions.findAllSlotsOfEach(all.stream().map(RecurringEventSeries::id).toList());
        for (RecurringEventSeries series : all) {
            List<CalendarEvent> edited = editedBySeries.getOrDefault(series.id(), List.of());
            if (!concerns.test(series.participantIds())) {
                // Someone else's series — but one of its occurrences may have been made the reader's own.
                edited.stream().filter(event -> concerns.test(event.participantIds())).forEach(plain::add);
                continue;
            }
            Set<LocalDate> off = new HashSet<>(cancelled.getOrDefault(series.id(), Set.of()));
            List<CalendarEvent> kept = new ArrayList<>();
            for (CalendarEvent event : edited) {
                if (concerns.test(event.participantIds())) {
                    kept.add(event);
                } else {
                    off.add(event.recurringOriginalDate());
                }
            }
            written.add(new FeedSeries(series, off, kept));
        }
    }

    private List<CalendarOccurrence> others(SpaceMembership reader, Set<CalendarSourceType> wanted,
                                            Predicate<List<UUID>> concerns) {
        LocalDate today = spaceToday.today(reader.spaceId());
        LocalDate from = today.minusMonths(MONTHS_BACK);
        LocalDate to = today.plusMonths(MONTHS_AHEAD);
        List<CalendarOccurrence> found = new ArrayList<>();
        for (CalendarSource source : sources) {
            CalendarSourceType type = source.type();
            if (type == CalendarSourceType.EVENT || type == CalendarSourceType.EXTERNAL || !wanted.contains(type)) {
                continue;
            }
            for (LocalDate start = from; !start.isAfter(to); start = start.plusDays(EventRecurrenceProjector.MAX_WINDOW_DAYS)) {
                LocalDate windowStart = start;
                LocalDate end = start.plusDays(EventRecurrenceProjector.MAX_WINDOW_DAYS - 1L);
                // An occurrence overlapping two windows is written once, from the window it starts in.
                source.occurrencesBetween(reader, windowStart, end.isAfter(to) ? to : end).stream()
                    .filter(occurrence -> !occurrence.startDate().isBefore(windowStart) || windowStart.equals(from))
                    .filter(occurrence -> concerns.test(occurrence.participantIds()))
                    .forEach(found::add);
            }
        }
        return found;
    }
}
