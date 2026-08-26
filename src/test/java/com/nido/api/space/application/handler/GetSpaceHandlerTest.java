package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSpaceHandlerTest {

    @Mock SpaceRepository spaceRepository;

    private GetSpaceHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetSpaceHandler(spaceRepository);
    }

    @Test
    void returns_the_detail_with_the_caller_role_and_the_member_count() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));
        when(spaceRepository.countMembers(spaceId)).thenReturn(3L);

        SpaceDetailView view = handler.get(spaceId, membership(SpaceRole.MEMBER));

        assertThat(view.id()).isEqualTo(spaceId);
        assertThat(view.name()).isEqualTo("Chez Valentin");
        assertThat(view.myRole()).isEqualTo(SpaceRole.MEMBER);
        assertThat(view.memberCount()).isEqualTo(3);
    }

    @Test
    void a_space_id_other_than_the_authorized_one_is_refused() {
        assertThatThrownBy(() -> handler.get(UUID.randomUUID(), membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.NotAMember.class);
        verifyNoInteractions(spaceRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }

    private Space shared() {
        return new Space(spaceId, SpaceType.SHARED, "Chez Valentin", null, "#c17a5c", "🏡", null, Instant.now());
    }
}
