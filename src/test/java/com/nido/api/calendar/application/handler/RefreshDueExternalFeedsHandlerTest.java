package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.ExternalFeedRefresher;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        ExternalFeed due = feed(10, NOW.minus(Duration.ofHours(8)));
        // Six hours and a half old, with a delay of its own of 45 minutes: not yet.
        ExternalFeed waiting = feed(45, NOW.minus(Duration.ofMinutes(390)));
        // The same age with a delay of 20 minutes: due.
        ExternalFeed dueWithShortDelay = feed(20, NOW.minus(Duration.ofMinutes(390)));
        stub(due, waiting, dueWithShortDelay);

        handler.refreshDue();

        verify(refresher).refresh(due);
        verify(refresher, never()).refresh(waiting);
        verify(refresher).refresh(dueWithShortDelay);
    }

    @Test
    void theRepositoryIsAskedForAtMostAHandfulOfTheOldestAttempts() {
        when(feeds.findDueIds(any(), anyInt())).thenReturn(List.of());

        handler.refreshDue();

        verify(feeds).findDueIds(NOW.minus(Duration.ofHours(6)), 40);
    }

    @Test
    void oneCalendarFailingDoesNotStopTheOthers() {
        ExternalFeed broken = feed(1, NOW.minus(Duration.ofHours(9)));
        ExternalFeed fine = feed(2, NOW.minus(Duration.ofHours(9)));
        stub(broken, fine);
        doThrow(new IllegalStateException("boom")).when(refresher).refresh(broken);

        handler.refreshDue();

        verify(refresher).refresh(fine);
    }

    @Test
    void aCalendarThatCannotBeReadBackDoesNotStopTheOthers() {
        ExternalFeed unreadable = feed(1, NOW.minus(Duration.ofHours(9)));
        ExternalFeed fine = feed(2, NOW.minus(Duration.ofHours(9)));
        when(feeds.findDueIds(any(), anyInt())).thenReturn(List.of(unreadable.id(), fine.id()));
        when(feeds.findById(unreadable.id())).thenThrow(new IllegalArgumentException("Illegal character in URI"));
        when(feeds.findById(fine.id())).thenReturn(Optional.of(fine));

        handler.refreshDue();

        verify(refresher, never()).refresh(unreadable);
        verify(refresher).refresh(fine);
    }

    @Test
    void aCalendarThatCannotBeReadBackIsMarkedTriedSoThatTheNextTicksAskTheOthersFirst() {
        ExternalFeed unreadable = feed(1, NOW.minus(Duration.ofHours(9)));
        when(feeds.findDueIds(any(), anyInt())).thenReturn(List.of(unreadable.id()));
        when(feeds.findById(unreadable.id())).thenThrow(new IllegalArgumentException("Illegal character in URI"));

        handler.refreshDue();

        verify(feeds).markAttempt(unreadable.id(), NOW);
    }

    @Test
    void aCalendarThatCannotBeMarkedEitherDoesNotStopTheOthers() {
        ExternalFeed unreadable = feed(1, NOW.minus(Duration.ofHours(9)));
        ExternalFeed fine = feed(2, NOW.minus(Duration.ofHours(9)));
        when(feeds.findDueIds(any(), anyInt())).thenReturn(List.of(unreadable.id(), fine.id()));
        when(feeds.findById(unreadable.id())).thenThrow(new IllegalArgumentException("Illegal character in URI"));
        when(feeds.findById(fine.id())).thenReturn(Optional.of(fine));
        doThrow(new IllegalStateException("database gone")).when(feeds).markAttempt(unreadable.id(), NOW);

        handler.refreshDue();

        verify(refresher).refresh(fine);
    }

    @Test
    void aCalendarRemovedMeanwhileIsSkipped() {
        ExternalFeed gone = feed(1, NOW.minus(Duration.ofHours(9)));
        ExternalFeed fine = feed(2, NOW.minus(Duration.ofHours(9)));
        stub(gone, fine);
        doThrow(new CalendarException.ExternalFeedNotFound()).when(refresher).refresh(gone);

        handler.refreshDue();

        verify(refresher).refresh(fine);
    }

    @Test
    void aTickStopsOnceItsTimeIsSpent() {
        AtomicReference<Instant> now = new AtomicReference<>(NOW);
        Clock moving = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        RefreshDueExternalFeedsHandler slow = new RefreshDueExternalFeedsHandler(feeds, refresher, moving);
        ExternalFeed[] due = new ExternalFeed[5];
        for (int i = 0; i < due.length; i++) {
            due[i] = feed(i, NOW.minus(Duration.ofHours(9)));
        }
        stub(due);
        // Each refresh takes three minutes: after three of them, eight minutes are gone.
        doAnswer(call -> {
            now.set(now.get().plus(Duration.ofMinutes(3)));
            return null;
        }).when(refresher).refresh(any());

        slow.refreshDue();

        verify(refresher, times(3)).refresh(any());
        verify(refresher, never()).refresh(due[3]);
    }

    private void stub(ExternalFeed... due) {
        when(feeds.findDueIds(any(), anyInt()))
            .thenReturn(Arrays.stream(due).map(ExternalFeed::id).toList());
        for (ExternalFeed feed : due) {
            when(feeds.findById(feed.id())).thenReturn(Optional.of(feed));
        }
    }

    /** The id decides the calendar's own delay: this one's is {@code delayMinutes} (UUID(0, n) hashes to n). */
    private static ExternalFeed feed(int delayMinutes, Instant lastAttempt) {
        return new ExternalFeed(new UUID(0, delayMinutes), UUID.randomUUID(), "x", URI.create("https://x.example/a.ics"),
            null, UUID.randomUUID(), lastAttempt, lastAttempt, lastAttempt, null, null, null);
    }
}
