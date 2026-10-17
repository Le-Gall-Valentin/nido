package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedCheck;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.nido.api.calendar.domain.model.FollowExternalFeedCommand;
import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowExternalFeedHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");
    private static final String ICS = "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n";

    private final ExternalFeedRepository feeds = mock(ExternalFeedRepository.class);
    private final ExternalFeedDownloader downloader = mock(ExternalFeedDownloader.class);
    private final IcalFeedChecker checker = mock(IcalFeedChecker.class);
    private final FollowExternalFeedHandler handler = new FollowExternalFeedHandler(feeds, downloader, checker,
        Clock.fixed(NOW, ZoneOffset.UTC));
    private final SpaceMembership member = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        SpaceRole.MEMBER, NOW);

    @Test
    void aCalendarIsFollowedUnderTheNameItGivesItself() {
        URI url = URI.create("https://calendar.google.com/x/basic.ics");
        when(downloader.download(url, null, null)).thenReturn(new FeedDownload.Downloaded(ICS, "\"v1\"", null));
        when(checker.check(ICS)).thenReturn(new FeedCheck(Optional.of("Vacances scolaires"), 10));
        ExternalFeed created = new ExternalFeed(UUID.randomUUID(), member.spaceId(), "Vacances scolaires", url, "event-cyan",
            member.userId(), NOW, NOW, NOW, null, "\"v1\"", null);
        when(feeds.create(any(), anyString(), any(), any(), any(), anyString(), any(), any(), any())).thenReturn(created);

        handler.follow(new FollowExternalFeedCommand("webcal://calendar.google.com/x/basic.ics", " ", "event-cyan"), member);

        verify(feeds).create(member.spaceId(), "Vacances scolaires", url, "event-cyan", member.userId(), ICS, "\"v1\"", null, NOW);
    }

    @Test
    void aCalendarNamingItselfNothingIsNamedAfterItsHost() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(ICS, null, null));
        when(checker.check(ICS)).thenReturn(new FeedCheck(Optional.empty(), 10));

        handler.follow(new FollowExternalFeedCommand("https://ecole.example/v.ics", null, null), member);

        verify(feeds).create(eq(member.spaceId()), eq("ecole.example"), any(), any(), any(), anyString(), any(), any(), any());
    }

    @Test
    void anAddressThatDoesNotAnswerACalendarIsNotKept() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TIMEOUT)));

        assertThatThrownBy(() -> handler.follow(new FollowExternalFeedCommand("https://slow.example/a.ics", "x", null), member))
            .isInstanceOfSatisfying(CalendarException.FeedUnavailable.class,
                e -> assertThat(e.error()).isEqualTo(FeedError.of(FeedErrorKind.TIMEOUT)));
        verify(feeds, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void aDownloadThatIsNotACalendarIsNotKeptEither() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded("<html>", null, null));
        when(checker.check("<html>")).thenThrow(new CalendarException.NotAnIcalFile());

        assertThatThrownBy(() -> handler.follow(new FollowExternalFeedCommand("https://x.example/a.ics", "x", null), member))
            .isInstanceOfSatisfying(CalendarException.FeedUnavailable.class,
                e -> assertThat(e.error()).isEqualTo(FeedError.of(FeedErrorKind.NOT_ICS)));
    }

    @Test
    void tenCalendarsPerSpaceAtMost() {
        when(feeds.countBySpaceId(member.spaceId())).thenReturn(10L);
        assertThatThrownBy(() -> handler.follow(new FollowExternalFeedCommand("https://x.example/a.ics", "x", null), member))
            .isInstanceOf(CalendarException.TooManyExternalFeeds.class);
        verify(downloader, never()).download(any(), any(), any());
    }

    @Test
    void aNameTheCalendarGivesItselfIsStrippedAndABlankOneIsNoName() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(ICS, null, null));
        when(checker.check(ICS)).thenReturn(new FeedCheck(Optional.of("  Club de judo \n"), 10));
        handler.follow(new FollowExternalFeedCommand("https://ecole.example/v.ics", null, null), member);
        verify(feeds).create(eq(member.spaceId()), eq("Club de judo"), any(), any(), any(), anyString(), any(), any(), any());

        when(checker.check(ICS)).thenReturn(new FeedCheck(Optional.of("   "), 10));
        handler.follow(new FollowExternalFeedCommand("https://ecole.example/v.ics", null, null), member);
        verify(feeds).create(eq(member.spaceId()), eq("ecole.example"), any(), any(), any(), anyString(), any(), any(), any());
    }

    @Test
    void aLongNameIsCutWithoutSplittingACharacter() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(ICS, null, null));
        when(checker.check(ICS)).thenReturn(new FeedCheck(Optional.empty(), 10));
        String name = "a".repeat(99) + "\uD83D\uDE00" + "b";

        handler.follow(new FollowExternalFeedCommand("https://ecole.example/v.ics", name, null), member);

        verify(feeds).create(eq(member.spaceId()), eq("a".repeat(99) + "\uD83D\uDE00"), any(), any(), any(), anyString(), any(), any(), any());
    }

    @Test
    void aCalendarTooHeavyToKeepInMemoryIsRefusedAsTooLarge() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(ICS, null, null));
        when(checker.check(ICS)).thenReturn(new FeedCheck(Optional.empty(), FeedCheck.MAX_WEIGHT + 1));

        assertThatThrownBy(() -> handler.follow(new FollowExternalFeedCommand("https://x.example/a.ics", "x", null), member))
            .isInstanceOfSatisfying(CalendarException.FeedUnavailable.class,
                e -> assertThat(e.error()).isEqualTo(FeedError.of(FeedErrorKind.TOO_LARGE)));
        verify(feeds, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
