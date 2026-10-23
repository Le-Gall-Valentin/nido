package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.CreateSubscriptionLinkCommand;
import com.nido.api.calendar.domain.model.CreatedSubscriptionLink;
import com.nido.api.calendar.domain.model.SubscriptionLink;
import com.nido.api.calendar.domain.port.out.PublicAddressPort;
import com.nido.api.calendar.domain.port.out.SubscriptionLinkRepository;
import com.nido.api.calendar.domain.port.out.SubscriptionTokenGeneratorPort;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateSubscriptionLinkHandlerTest {

    private static final String TOKEN = "t".repeat(43);

    private final SubscriptionLinkRepository links = mock(SubscriptionLinkRepository.class);
    private final PublicAddressPort publicAddress = mock(PublicAddressPort.class);
    private final SubscriptionTokenGeneratorPort tokens = () -> TOKEN;
    private final CreateSubscriptionLinkHandler handler = new CreateSubscriptionLinkHandler(links, publicAddress, tokens);

    private final SpaceMembership viewer = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        SpaceRole.VIEWER, Instant.now());
    private final CreateSubscriptionLinkCommand command =
        new CreateSubscriptionLinkCommand(" Téléphone ", Set.of(CalendarSourceType.EVENT), true);

    private SubscriptionLink link() {
        return new SubscriptionLink(UUID.randomUUID(), viewer.spaceId(), viewer.userId(), "Téléphone",
            Set.of(CalendarSourceType.EVENT), true, Instant.now(), null);
    }

    @Test
    void withoutAPublicAddressNoAddressIsMade() {
        when(publicAddress.publicUrl()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.create(command, viewer)).isInstanceOf(CalendarException.PublicUrlMissing.class);
        verifyNoInteractions(links);
    }

    @Test
    void anAddressBeginsWithThePublicAddressAndOnlyItsDigestIsKept() {
        when(publicAddress.publicUrl()).thenReturn(Optional.of("https://nido.example"));
        SubscriptionLink made = link();
        when(links.createUnlessHolding(anyInt(), any(), any(), anyString(), any(), anyBoolean(), anyString()))
            .thenReturn(Optional.of(made));

        CreatedSubscriptionLink created = handler.create(command, viewer);

        assertThat(created.url()).isEqualTo("https://nido.example/ical/" + TOKEN + ".ics");
        assertThat(created.webcalUrl()).isEqualTo("webcal://nido.example/ical/" + TOKEN + ".ics");
        assertThat(created.link()).isEqualTo(made);
        verify(links).createUnlessHolding(10, viewer.spaceId(), viewer.userId(), "Téléphone", Set.of(CalendarSourceType.EVENT),
            true, CalendarDigests.sha256Hex(TOKEN));
    }

    @Test
    void tenAddressesOfAMemberInASpaceAtMost() {
        when(publicAddress.publicUrl()).thenReturn(Optional.of("https://nido.example"));
        when(links.createUnlessHolding(anyInt(), any(), any(), anyString(), any(), anyBoolean(), anyString()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.create(command, viewer)).isInstanceOf(CalendarException.TooManySubscriptionLinks.class);
    }

    @Test
    void aSourceThatCannotBeExportedIsRefusedBeforeAnythingIsMade() {
        when(publicAddress.publicUrl()).thenReturn(Optional.of("https://nido.example"));

        assertThatThrownBy(() -> handler.create(new CreateSubscriptionLinkCommand("x", Set.of(), false), viewer))
            .isInstanceOf(CalendarException.InvalidSources.class);
        verifyNoInteractions(links);
    }

    @Test
    void theAddressJustMadeIsNeverPrinted() {
        when(publicAddress.publicUrl()).thenReturn(Optional.of("https://nido.example"));
        when(links.createUnlessHolding(anyInt(), any(), any(), anyString(), any(), anyBoolean(), anyString()))
            .thenReturn(Optional.of(link()));

        assertThat(handler.create(command, viewer).toString()).doesNotContain(TOKEN);
    }
}
