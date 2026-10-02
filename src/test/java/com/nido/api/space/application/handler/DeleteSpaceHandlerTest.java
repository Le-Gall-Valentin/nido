package com.nido.api.space.application.handler;

import org.mockito.InOrder;
import com.nido.api.space.application.service.SpaceNotifier;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceAppearance;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteSpaceHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceCommandPort spaceCommandPort;
    @Mock SpaceNotifier spaceNotifier;

    private DeleteSpaceHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteSpaceHandler(spaceRepository, spaceCommandPort, spaceNotifier);
    }

    @Test
    void the_members_are_told_before_the_space_and_its_memberships_go() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));

        handler.delete(membership(SpaceRole.OWNER));

        InOrder order = inOrder(spaceNotifier, spaceCommandPort);
        order.verify(spaceNotifier).spaceDeleted(spaceId, userId);
        order.verify(spaceCommandPort).delete(spaceId);
    }

    @Test
    void an_admin_cannot_delete_a_group() {
        assertThatThrownBy(() -> handler.delete(membership(SpaceRole.ADMIN)))
            .isInstanceOf(SpaceException.OwnerRequired.class);
        verify(spaceCommandPort, never()).delete(spaceId);
        verifyNoInteractions(spaceNotifier);
    }

    @Test
    void the_personal_space_cannot_be_deleted() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(personal()));

        assertThatThrownBy(() -> handler.delete(membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.PersonalSpaceImmutable.class);
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
