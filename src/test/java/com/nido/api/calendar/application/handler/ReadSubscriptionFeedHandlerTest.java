package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarFeedAssembler;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CalendarFeed;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.SubscriptionLink;
import com.nido.api.calendar.domain.model.SubscriptionTokens;
import com.nido.api.calendar.domain.port.out.AccountLanguage;
import com.nido.api.calendar.domain.port.out.ActiveAccounts;
import com.nido.api.calendar.domain.port.out.IcalWriter;
import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReadSubscriptionFeedHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final SubscriptionLinkRepository links = mock(SubscriptionLinkRepository.class);
    private final SpaceMembershipPort memberships = mock(SpaceMembershipPort.class);
    private final CalendarFeedAssembler assembler = mock(CalendarFeedAssembler.class);
    private final IcalWriter writer = mock(IcalWriter.class);
    private final AccountLanguage language = mock(AccountLanguage.class);
    private final ActiveAccounts accounts = mock(ActiveAccounts.class);
    private final ReadSubscriptionFeedHandler handler = new ReadSubscriptionFeedHandler(links, memberships, assembler,
        writer, language, accounts, Clock.fixed(NOW, ZoneOffset.UTC));

    private final String token = SubscriptionTokens.newToken();
    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    {
        when(accounts.isActive(userId)).thenReturn(true);
    }

    @Test
    void aMemberStillInTheSpaceIsReadAsTheyRead() {
        SubscriptionLink link = link(null);
        SpaceMembership member = new SpaceMembership(UUID.randomUUID(), spaceId, userId, SpaceRole.VIEWER, NOW);
        CalendarFeed feed = new CalendarFeed("Chez nous", ZoneId.of("Europe/Paris"), List.of(), List.of(), List.of());
        when(links.findByTokenHash(CalendarDigests.sha256Hex(token))).thenReturn(Optional.of(link));
        when(memberships.find(spaceId, userId)).thenReturn(Optional.of(member));
        when(assembler.assemble(member, Set.of(CalendarSourceType.EVENT), true)).thenReturn(feed);
        when(language.of(userId)).thenReturn(Locale.FRENCH);
        when(writer.write(feed, Locale.FRENCH)).thenReturn("BEGIN:VCALENDAR");

        assertThat(handler.read(token)).contains("BEGIN:VCALENDAR");
        verify(links).markRead(link.id(), NOW);
    }

    @Test
    void aReadWithinTheHourDoesNotWriteAgain() {
        SubscriptionLink link = link(NOW.minus(Duration.ofMinutes(20)));
        when(links.findByTokenHash(anyString())).thenReturn(Optional.of(link));
        when(memberships.find(spaceId, userId)).thenReturn(Optional.of(
            new SpaceMembership(UUID.randomUUID(), spaceId, userId, SpaceRole.VIEWER, NOW)));
        when(writer.write(any(), any())).thenReturn("BEGIN:VCALENDAR");

        handler.read(token);

        verify(links, never()).markRead(any(), any());
    }

    @Test
    void anAddressWhoseOwnerLeftTheSpaceReadsNothing() {
        when(links.findByTokenHash(anyString())).thenReturn(Optional.of(link(null)));
        when(memberships.find(spaceId, userId)).thenReturn(Optional.empty());

        assertThat(handler.read(token)).isEmpty();
        verify(assembler, never()).assemble(any(), any(), anyBoolean());
    }

    @Test
    void anAddressWhoseOwnerIsDeactivatedReadsNothing() {
        when(links.findByTokenHash(anyString())).thenReturn(Optional.of(link(null)));
        when(accounts.isActive(userId)).thenReturn(false);

        assertThat(handler.read(token)).isEmpty();
        verify(memberships, never()).find(any(), any());
        verify(assembler, never()).assemble(any(), any(), anyBoolean());
        verify(links, never()).markRead(any(), any());
    }

    @Test
    void anUnknownTokenReadsNothingAndLooksNoMembershipUp() {
        when(links.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThat(handler.read(token)).isEmpty();
        verify(memberships, never()).find(any(), any());
        verify(assembler, never()).assemble(any(), any(), anyBoolean());
    }

    @Test
    void aReadMoreThanAnHourAfterTheLastIsMarkedAgain() {
        SubscriptionLink link = link(NOW.minus(Duration.ofMinutes(61)));
        when(links.findByTokenHash(anyString())).thenReturn(Optional.of(link));
        when(memberships.find(spaceId, userId)).thenReturn(Optional.of(
            new SpaceMembership(UUID.randomUUID(), spaceId, userId, SpaceRole.VIEWER, NOW)));
        when(writer.write(any(), any())).thenReturn("BEGIN:VCALENDAR");

        handler.read(token);

        verify(links).markRead(link.id(), NOW);
    }

    @Test
    void aTokenThatCannotBeOneIsNotLookedUp() {
        assertThat(handler.read("../etc/passwd")).isEmpty();
        verify(links, never()).findByTokenHash(any());
    }

    private SubscriptionLink link(Instant lastReadAt) {
        return new SubscriptionLink(UUID.randomUUID(), spaceId, userId, "iPhone", Set.of(CalendarSourceType.EVENT), true,
            NOW.minus(Duration.ofDays(3)), lastReadAt);
    }
}
