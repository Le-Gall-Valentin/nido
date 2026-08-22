package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
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
class DeleteEmptySpaceHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceCommandPort spaceCommandPort;

    private DeleteEmptySpaceHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteEmptySpaceHandler(spaceRepository, spaceCommandPort);
    }

    @Test
    void deletes_a_space_that_has_no_member_left() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));
        when(spaceRepository.countMembers(spaceId)).thenReturn(0L);

        handler.delete(spaceId, adminId);

        verify(spaceCommandPort).delete(spaceId);
    }

    @Test
    void refuses_a_space_that_still_has_members() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));
        when(spaceRepository.countMembers(spaceId)).thenReturn(2L);

        assertThatThrownBy(() -> handler.delete(spaceId, adminId))
            .isInstanceOf(SpaceException.SpaceNotEmpty.class);
        verify(spaceCommandPort, never()).delete(spaceId);
    }

    @Test
    void an_unknown_space_is_not_found() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.delete(spaceId, adminId))
            .isInstanceOf(SpaceException.SpaceNotFound.class);
    }

    private Space shared() {
        return new Space(spaceId, SpaceType.SHARED, "Chez Valentin", null,
            "#c17a5c", "🏡", null, Instant.now());
    }
}
