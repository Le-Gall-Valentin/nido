package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceAppearance;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveSpaceHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceMembershipPort spaceMembershipPort;

    private LeaveSpaceHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new LeaveSpaceHandler(spaceRepository, spaceMembershipPort);
    }

    @Test
    void a_member_can_leave() {
        SpaceMembership membership = membership(SpaceRole.MEMBER);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));

        handler.leave(membership);

        verify(spaceMembershipPort).remove(membership.id());
    }

    @Test
    void the_last_owner_must_transfer_first() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));

        assertThatThrownBy(() -> handler.leave(membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.LastOwnerCannotLeave.class);
    }

    @Test
    void nobody_leaves_their_personal_space() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(personal()));

        assertThatThrownBy(() -> handler.leave(membership(SpaceRole.OWNER)))
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
