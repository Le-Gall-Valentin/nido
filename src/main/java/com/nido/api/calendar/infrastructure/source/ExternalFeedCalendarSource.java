package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.calendar.infrastructure.ical.Ical4jFeedReaderAdapter;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The calendars a space follows, read-only. A calendar's file is read from the database only when it is not in memory
 * already — the list of calendars, read at every calendar read, never carries the files.
 *
 * <p>A calendar that cannot be read (its seal, its content) costs only itself: the others and the other sources answer.
 */
@Component
public class ExternalFeedCalendarSource implements CalendarSource {

    private static final Logger log = LoggerFactory.getLogger(ExternalFeedCalendarSource.class);

    private final ExternalFeedRepository feeds;
    private final Ical4jFeedReaderAdapter reader;
    private final GetSpaceUseCase spaces;
    private final AccountLanguage language;

    public ExternalFeedCalendarSource(ExternalFeedRepository feeds, Ical4jFeedReaderAdapter reader,
                                      GetSpaceUseCase spaces, AccountLanguage language) {
        this.feeds = feeds;
        this.reader = reader;
        this.spaces = spaces;
        this.language = language;
    }

    @Override
    public CalendarSourceType type() {
        return CalendarSourceType.EXTERNAL;
    }

    @Override
    public List<CalendarOccurrence> occurrencesBetween(SpaceMembership caller, LocalDate from, LocalDate to) {
        List<ExternalFeed> followed = feeds.findBySpaceId(caller.spaceId());
        if (followed.isEmpty()) {
            return List.of();
        }
        ZoneId zone = spaces.get(caller.spaceId(), caller).timezone();
        Locale locale = language.of(caller.userId());
        List<CalendarOccurrence> found = new ArrayList<>();
        for (ExternalFeed feed : followed) {
            try {
                found.addAll(reader.occurrencesOf(feed, () -> feeds.loadContent(feed.id()), zone, locale, from, to));
            } catch (RuntimeException unreadable) {
                // The feed id only: the address often holds a secret. Whatever a third-party parser or the unsealing
                // throws on one calendar must not take the view down.
                log.warn("Followed calendar {} skipped: {}", feed.id(), unreadable.getClass().getSimpleName());
            }
        }
        return found;
    }
}
