package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceAppearance;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.model.UpdateSpaceCommand;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSpaceHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceCommandPort spaceCommandPort;

    private UpdateSpaceHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateSpaceHandler(spaceRepository, spaceCommandPort);
    }

    @Test
    void an_admin_can_rename_a_group() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));

        handler.update(command(), membership(SpaceRole.ADMIN));

        verify(spaceCommandPort).update(command());
    }

    @Test
    void a_member_cannot_rename_a_group() {
        assertThatThrownBy(() -> handler.update(command(), membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(spaceCommandPort, never()).update(command());
    }

    @Test
    void the_personal_space_cannot_be_renamed() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(personal()));

        assertThatThrownBy(() -> handler.update(command(), membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.PersonalSpaceImmutable.class);
    }

    @Test
    void a_command_targeting_another_space_than_the_authorized_one_is_refused() {
        UpdateSpaceCommand elsewhere = new UpdateSpaceCommand(UUID.randomUUID(), "Nouveau nom", null, "#4a7fa0", "🏠", null);

        assertThatThrownBy(() -> handler.update(elsewhere, membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.NotAMember.class);
        verify(spaceCommandPort, never()).update(elsewhere);
    }

    @Test
    void a_personal_space_accepts_a_change_of_calendar() {
        // It has no name, colour or glyph to change — that is deliberate and stays so — but it keeps
        // a calendar like any other space, and its owner is the only person that calendar concerns.
        // Refusing every update was the coarse version of "its identity is fixed".
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(personal()));
        UpdateSpaceCommand zoneOnly = new UpdateSpaceCommand(
            spaceId, null, null, null, null, ZoneId.of("America/Toronto"));

        handler.update(zoneOnly, membership(SpaceRole.OWNER));

        verify(spaceCommandPort).update(zoneOnly);
    }

    @Test
    void a_personal_space_still_refuses_a_change_of_identity() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(personal()));

        assertThatThrownBy(() -> handler.update(command(), membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.PersonalSpaceImmutable.class);
        verify(spaceCommandPort, never()).update(any());
    }

    @Test
    void a_personal_space_refuses_a_rename_hidden_behind_a_zone_change() {
        // The one that matters: allowing the zone must not open a door for the rest of the payload.
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(personal()));
        UpdateSpaceCommand smuggled = new UpdateSpaceCommand(
            spaceId, "Renamed", null, null, null, ZoneId.of("America/Toronto"));

        assertThatThrownBy(() -> handler.update(smuggled, membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.PersonalSpaceImmutable.class);
        verify(spaceCommandPort, never()).update(any());
    }

    private UpdateSpaceCommand command() {
        return new UpdateSpaceCommand(spaceId, "Nouveau nom", "Nouvelle description", "#4a7fa0", "🏠", null);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }

    private Space shared() {
        return new Space(spaceId, SpaceType.SHARED, "Chez Valentin", null, "#c17a5c", "🏡", null, ZoneId.of("Europe/Paris"), Instant.now());
    }

    private Space personal() {
        return new Space(spaceId, SpaceType.PERSONAL, "Perso", null, SpaceAppearance.PERSONAL_ACCENT, SpaceAppearance.PERSONAL_GLYPH, userId, ZoneId.of("Europe/Paris"), Instant.now());
    }
}
