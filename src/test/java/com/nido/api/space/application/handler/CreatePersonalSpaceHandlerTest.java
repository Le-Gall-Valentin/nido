package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceAppearance;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePersonalSpaceHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceCommandPort spaceCommandPort;
    @Mock SpaceMembershipPort spaceMembershipPort;

    private CreatePersonalSpaceHandler handler;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreatePersonalSpaceHandler(spaceRepository, spaceCommandPort, spaceMembershipPort);
    }

    @Test
    void creates_the_space_and_its_owner_membership() {
        Space created = personalSpace();
        when(spaceCommandPort.createPersonal(userId)).thenReturn(created);

        UUID result = handler.createFor(userId);

        assertThat(result).isEqualTo(created.id());
        verify(spaceMembershipPort).add(created.id(), userId, SpaceRole.OWNER);
    }

    @Test
    void is_idempotent_when_the_personal_space_already_exists() {
        Space existing = personalSpace();
        when(spaceCommandPort.createPersonal(userId))
            .thenThrow(new SpaceException.PersonalSpaceAlreadyExists());
        when(spaceRepository.findPersonalOwnedBy(userId)).thenReturn(Optional.of(existing));

        UUID result = handler.createFor(userId);

        assertThat(result).isEqualTo(existing.id());
        verify(spaceMembershipPort, never()).add(existing.id(), userId, SpaceRole.OWNER);
    }

    private Space personalSpace() {
        return new Space(UUID.randomUUID(), SpaceType.PERSONAL, "Perso", null,
            SpaceAppearance.PERSONAL_ACCENT, SpaceAppearance.PERSONAL_GLYPH, userId, Instant.now());
    }
}
