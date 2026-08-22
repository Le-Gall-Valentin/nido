package com.nido.api.space.application.handler;

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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteSpaceHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceCommandPort spaceCommandPort;

    private DeleteSpaceHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteSpaceHandler(spaceRepository, spaceCommandPort);
    }

    @Test
    void the_owner_can_delete_a_group() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));

        handler.delete(membership(SpaceRole.OWNER));

        verify(spaceCommandPort).delete(spaceId);
    }

    @Test
    void an_admin_cannot_delete_a_group() {
        assertThatThrownBy(() -> handler.delete(membership(SpaceRole.ADMIN)))
            .isInstanceOf(SpaceException.OwnerRequired.class);
        verify(spaceCommandPort, never()).delete(spaceId);
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
        return new Space(spaceId, SpaceType.SHARED, "Chez Valentin", null, "#c17a5c", "🏡", null, Instant.now());
    }

    private Space personal() {
        return new Space(spaceId, SpaceType.PERSONAL, "Perso", null,
            SpaceAppearance.PERSONAL_ACCENT, SpaceAppearance.PERSONAL_GLYPH, userId, Instant.now());
    }
}
