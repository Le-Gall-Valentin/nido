package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

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

    private final ExternalFeedRepository feeds = mock(ExternalFeedRepository.class);
    private final ExternalFeedDownloader downloader = mock(ExternalFeedDownloader.class);
    private final IcalFeedChecker checker = mock(IcalFeedChecker.class);
    private final ExternalFeedRefresher refresher = new ExternalFeedRefresher(feeds, downloader, checker,
        Clock.fixed(NOW, ZoneOffset.UTC));
    private final URI url = URI.create("https://x.example/a.ics");
    private final ExternalFeed feed = new ExternalFeed(UUID.randomUUID(), UUID.randomUUID(), "x", url, null,
        UUID.randomUUID(), NOW, NOW, NOW, null, "\"v1\"", "yesterday");

    ExternalFeedRefresherTest() {
        when(feeds.findById(feed.id())).thenReturn(Optional.of(feed));
    }

    @Test
    void theAttemptIsRecordedBeforeTheAddressIsAsked() {
        when(downloader.download(url, "\"v1\"", "yesterday")).thenReturn(new FeedDownload.Downloaded(ICS, "\"v2\"", null));
        when(checker.calendarName(ICS)).thenReturn(Optional.empty());

        refresher.refresh(feed);

        InOrder order = inOrder(feeds, downloader);
        order.verify(feeds).markAttempt(feed.id(), NOW);
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
        when(checker.calendarName("<html>")).thenThrow(new CalendarException.NotAnIcalFile());

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
}
