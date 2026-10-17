package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.ExternalFeedRefresher;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshDueExternalFeedsHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final ExternalFeedRepository feeds = mock(ExternalFeedRepository.class);
    private final ExternalFeedRefresher refresher = mock(ExternalFeedRefresher.class);
    private final RefreshDueExternalFeedsHandler handler = new RefreshDueExternalFeedsHandler(feeds, refresher,
        Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void aCalendarIsReadAgainSixHoursAndItsOwnDelayAfterItsLastAttempt() {
        ExternalFeed due = feed(NOW.minus(Duration.ofHours(8)));
        ExternalFeed justTried = feed(NOW.minus(Duration.ofHours(6)).plus(Duration.ofSeconds(1)));
        when(feeds.findAttemptedBefore(NOW.minus(Duration.ofHours(6)))).thenReturn(List.of(due));

        handler.refreshDue();

        verify(refresher).refresh(due);
        verify(refresher, never()).refresh(justTried);
    }

    @Test
    void oneCalendarFailingDoesNotStopTheOthers() {
        ExternalFeed broken = feed(NOW.minus(Duration.ofHours(9)));
        ExternalFeed fine = feed(NOW.minus(Duration.ofHours(9)));
        when(feeds.findAttemptedBefore(any())).thenReturn(List.of(broken, fine));
        doThrow(new IllegalStateException("boom")).when(refresher).refresh(broken);

        handler.refreshDue();

        verify(refresher).refresh(fine);
    }

    private static ExternalFeed feed(Instant lastAttempt) {
        return new ExternalFeed(UUID.randomUUID(), UUID.randomUUID(), "x", URI.create("https://x.example/a.ics"), null,
            UUID.randomUUID(), lastAttempt, lastAttempt, lastAttempt, null, null, null);
    }
}
