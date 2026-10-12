package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClearOpeningBalanceHandlerTest {

    @Mock OpeningBalanceRepository openingBalances;
    private final UUID spaceId = UUID.randomUUID();

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_removes_it() {
        new ClearOpeningBalanceHandler(openingBalances).clear(spaceId, membership(SpaceRole.MEMBER));

        verify(openingBalances).deleteBySpaceId(spaceId);
    }

    @Test
    void a_viewer_cannot_remove_it() {
        assertThatThrownBy(() -> new ClearOpeningBalanceHandler(openingBalances).clear(spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(openingBalances, never()).deleteBySpaceId(any());
    }
}
