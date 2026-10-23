package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.FeedToShow;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.ExternalFeedContentRepository;
import com.nido.api.calendar.infrastructure.ical.Ical4jFeedReaderAdapter;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalFeedCalendarSourceTest {

    private static final String HOLIDAY = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\n"
        + "BEGIN:VEVENT\r\nUID:a\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Vacances\r\n"
        + "DTSTART;VALUE=DATE:20261017\r\nDTEND;VALUE=DATE:20261018\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";

    private final ExternalFeedContentRepository feeds = mock(ExternalFeedContentRepository.class);
    private final GetSpaceUseCase spaces = mock(GetSpaceUseCase.class);
    private final AccountLanguage language = mock(AccountLanguage.class);
    private final ExternalFeedCalendarSource source =
        new ExternalFeedCalendarSource(feeds, new Ical4jFeedReaderAdapter(), spaces, language);
    private final SpaceMembership caller = new SpaceMembership(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());

    private FeedToShow feed() {
        return new FeedToShow(UUID.randomUUID(), "event-cyan", Instant.parse("2026-10-13T00:00:00Z"));
    }

    private void spaceInParis() {
        when(spaces.get(any(), any())).thenReturn(new SpaceDetailView(caller.spaceId(), SpaceType.SHARED, "Chez nous",
            null, null, null, java.time.ZoneId.of("Europe/Paris"), SpaceRole.VIEWER, 2));
        when(language.of(any())).thenReturn(Locale.FRENCH);
    }

    @Test
    void aSpaceFollowingNothingCostsNothing() {
        when(feeds.findShownBySpaceId(caller.spaceId())).thenReturn(List.of());

        assertThat(source.occurrencesBetween(caller, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).isEmpty();
        verify(spaces, never()).get(any(), any());
    }

    @Test
    void aCalendarThatCannotBeReadIsSkippedAndTheOthersStillAnswer() {
        FeedToShow broken = feed();
        FeedToShow fine = feed();
        spaceInParis();
        when(feeds.findShownBySpaceId(caller.spaceId())).thenReturn(List.of(broken, fine));
        when(feeds.loadContent(broken.id())).thenReturn("<html>not a calendar</html>");
        when(feeds.loadContent(fine.id())).thenReturn(HOLIDAY);

        List<CalendarOccurrence> found = source.occurrencesBetween(caller, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(found).extracting(CalendarOccurrence::title).containsExactly("Vacances");
    }

    @Test
    void aCalendarWhoseContentCannotBeLoadedIsSkippedToo() {
        FeedToShow unsealable = feed();
        FeedToShow fine = feed();
        spaceInParis();
        when(feeds.findShownBySpaceId(caller.spaceId())).thenReturn(List.of(unsealable, fine));
        when(feeds.loadContent(unsealable.id())).thenThrow(new IllegalStateException("cannot unseal"));
        when(feeds.loadContent(fine.id())).thenReturn(HOLIDAY);

        assertThat(source.occurrencesBetween(caller, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).hasSize(1);
    }
}
