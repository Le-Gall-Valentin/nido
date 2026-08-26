package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSharedSpaceHandlerTest {

    @Mock SpaceCommandPort spaceCommandPort;
    @Mock SpaceMembershipPort spaceMembershipPort;

    private CreateSharedSpaceHandler handler;

    private final UUID creator = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateSharedSpaceHandler(spaceCommandPort, spaceMembershipPort);
    }

    @Test
    void the_creator_becomes_the_owner() {
        Space created = new Space(UUID.randomUUID(), SpaceType.SHARED, "Chez Valentin", null,
            "#c17a5c", "🏡", null, Instant.now());
        when(spaceCommandPort.createShared(any(CreateSharedSpaceCommand.class))).thenReturn(created);

        Space result = handler.create(
            new CreateSharedSpaceCommand("Chez Valentin", null, "#c17a5c", "🏡", creator));

        assertThat(result).isEqualTo(created);
        verify(spaceMembershipPort).add(created.id(), creator, SpaceRole.OWNER);
    }
}
