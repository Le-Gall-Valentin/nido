package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedCheck;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.calendar.domain.port.out.ExternalFeedRefreshRepository;
import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalFeedRefresherTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");
    private static final String ICS = "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n";

    private final ExternalFeedRefreshRepository feeds = mock(ExternalFeedRefreshRepository.class);
    private final ExternalFeedDownloader downloader = mock(ExternalFeedDownloader.class);
    private final IcalFeedChecker checker = mock(IcalFeedChecker.class);
    private final ExternalFeedRefresher refresher = new ExternalFeedRefresher(feeds, downloader, checker,
        Clock.fixed(NOW, ZoneOffset.UTC));
    private final URI url = URI.create("https://x.example/a.ics");
    private final ExternalFeed feed = new ExternalFeed(UUID.randomUUID(), UUID.randomUUID(), "x", url, null,
        UUID.randomUUID(), NOW, NOW, NOW, null, "\"v1\"", "yesterday");

    ExternalFeedRefresherTest() {
        when(feeds.findById(feed.id())).thenReturn(Optional.of(feed));
        when(feeds.claimAttempt(feed.id(), NOW, NOW.minus(ExternalFeedRefresher.AT_MOST_EVERY))).thenReturn(true);
    }

    @Test
    void aCalendarAskedWithinTheMinuteIsRefusedAndNotAskedAgain() {
        when(feeds.claimAttempt(feed.id(), NOW, NOW.minus(ExternalFeedRefresher.AT_MOST_EVERY))).thenReturn(false);

        assertThatThrownBy(() -> refresher.refresh(feed)).isInstanceOf(CalendarException.RefreshTooSoon.class);
        verify(downloader, never()).download(any(), any(), any());
    }

    @Test
    void theAttemptIsClaimedBeforeTheAddressIsAsked() {
        when(downloader.download(url, "\"v1\"", "yesterday")).thenReturn(new FeedDownload.Downloaded(ICS, "\"v2\"", null));
        when(checker.check(ICS)).thenReturn(new FeedCheck(Optional.empty(), 10));

        refresher.refresh(feed);

        InOrder order = inOrder(feeds, downloader);
        order.verify(feeds).claimAttempt(feed.id(), NOW, NOW.minus(ExternalFeedRefresher.AT_MOST_EVERY));
        order.verify(downloader).download(url, "\"v1\"", "yesterday");
        order.verify(feeds).recordSuccess(feed.id(), ICS, "\"v2\"", null, NOW);
    }

    @Test
    void aCalendarNotModifiedKeepsItsContentAndRecordsTheAttempt() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.NotModified());

        refresher.refresh(feed);

        verify(feeds).recordNotModified(feed.id(), NOW);
        verify(feeds, never()).recordSuccess(any(), anyString(), any(), any(), any());
        verify(feeds, never()).recordFailure(any(), any(), any());
    }

    @Test
    void aFileThatIsNoLongerACalendarKeepsTheOldOneAndRecordsWhy() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded("<html>", null, null));
        when(checker.check("<html>")).thenThrow(new CalendarException.NotAnIcalFile());

        refresher.refresh(feed);

        verify(feeds).recordFailure(feed.id(), FeedError.of(FeedErrorKind.NOT_ICS), NOW);
        verify(feeds, never()).recordSuccess(any(), anyString(), any(), any(), any());
    }

    @Test
    void anAddressThatFailsKeepsTheOldCalendarAndRecordsWhy() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Failed(FeedError.http(500)));

        refresher.refresh(feed);

        verify(feeds).recordFailure(feed.id(), FeedError.http(500), NOW);
        verify(feeds, never()).recordSuccess(any(), anyString(), any(), any(), any());
    }

    @Test
    void aCalendarTooHeavyToKeepInMemoryKeepsTheOldOneAndRecordsWhy() {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(ICS, null, null));
        when(checker.check(ICS)).thenReturn(new FeedCheck(Optional.empty(), FeedCheck.MAX_WEIGHT + 1));

        refresher.refresh(feed);

        verify(feeds).recordFailure(feed.id(), FeedError.of(FeedErrorKind.TOO_LARGE), NOW);
        verify(feeds, never()).recordSuccess(any(), anyString(), any(), any(), any());
    }
}
