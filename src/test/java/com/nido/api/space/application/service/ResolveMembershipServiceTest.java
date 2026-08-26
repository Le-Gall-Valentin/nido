package com.nido.api.space.application.service;

import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveMembershipServiceTest {

    @Mock SpaceMembershipPort spaceMembershipPort;

    private ResolveMembershipService service;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ResolveMembershipService(spaceMembershipPort);
    }

    @Test
    void returns_the_membership_of_a_member() {
        SpaceMembership membership =
            new SpaceMembership(UUID.randomUUID(), spaceId, userId, SpaceRole.MEMBER, Instant.now());
        when(spaceMembershipPort.find(spaceId, userId)).thenReturn(Optional.of(membership));

        assertThat(service.resolve(spaceId, userId)).isEqualTo(membership);
    }

    @Test
    void a_stranger_gets_not_a_member_which_the_web_layer_turns_into_404() {
        when(spaceMembershipPort.find(spaceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(spaceId, userId))
            .isInstanceOf(SpaceException.NotAMember.class);
    }
}
