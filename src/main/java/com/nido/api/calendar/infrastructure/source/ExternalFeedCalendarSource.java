package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.FeedToShow;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.calendar.domain.port.out.ExternalFeedContentRepository;
import com.nido.api.calendar.domain.port.out.FeedOccurrenceReader;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * The calendars a space follows, read-only. What a read needs of them opens nothing sealed — neither their addresses nor
 * their names —, and a calendar's file is read from the database only when it is not in memory already.
 *
 * <p>A calendar that cannot be read (its seal, its content) costs only itself: the reader leaves it out and says so.
 */
@Component
public class ExternalFeedCalendarSource implements CalendarSource {

    private final ExternalFeedContentRepository feeds;
    private final FeedOccurrenceReader reader;
    private final GetSpaceUseCase spaces;
    private final AccountLanguage language;

    public ExternalFeedCalendarSource(ExternalFeedContentRepository feeds, FeedOccurrenceReader reader,
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
        List<FeedToShow> followed = feeds.findShownBySpaceId(caller.spaceId());
        if (followed.isEmpty()) {
            return List.of();
        }
        ZoneId zone = spaces.get(caller.spaceId(), caller).timezone();
        Locale locale = language.of(caller.userId());
        // One call for all: they share one budget.
        return reader.occurrencesOf(followed, feeds::loadContent, zone, locale, from, to);
    }
}
