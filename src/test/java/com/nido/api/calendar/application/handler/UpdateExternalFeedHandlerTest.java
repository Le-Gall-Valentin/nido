package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.ExternalFeedLookup;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.UpdateExternalFeedCommand;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateExternalFeedHandlerTest {

    private final ExternalFeedRepository feeds = mock(ExternalFeedRepository.class);
    private final UpdateExternalFeedHandler handler = new UpdateExternalFeedHandler(feeds, new ExternalFeedLookup(feeds));

    private final SpaceMembership member = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        SpaceRole.MEMBER, Instant.now());
    private final ExternalFeed feed = new ExternalFeed(UUID.randomUUID(), member.spaceId(), "Club",
        URI.create("https://club.example/c.ics"), "event-cyan", member.userId(), Instant.EPOCH, Instant.EPOCH, Instant.EPOCH,
        null, null, null);

    UpdateExternalFeedHandlerTest() {
        when(feeds.findById(feed.id())).thenReturn(Optional.of(feed));
    }

    @Test
    void aNameAndAColourAreKeptStripped() {
        handler.update(feed.id(), new UpdateExternalFeedCommand("  Club de foot  ", "event-brique"), member);

        verify(feeds).update(feed.id(), "Club de foot", "event-brique");
    }

    @Test
    void noNameOrABlankOneKeepsTheOneItHas() {
        handler.update(feed.id(), new UpdateExternalFeedCommand(null, "event-brique"), member);
        handler.update(feed.id(), new UpdateExternalFeedCommand("   ", null), member);

        verify(feeds).update(feed.id(), "Club", "event-brique");
        verify(feeds).update(feed.id(), "Club", ExternalFeed.DEFAULT_COLOR);
    }

    @Test
    void aNameTooLongIsCutWithoutCuttingACharacterInTwo() {
        handler.update(feed.id(), new UpdateExternalFeedCommand("a".repeat(99) + "😀😀", null), member);

        verify(feeds).update(feed.id(), "a".repeat(99) + "😀", ExternalFeed.DEFAULT_COLOR);
    }
}
