package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.service.BalanceNotifier;
import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClearOpeningBalanceHandlerTest {

    @Mock OpeningBalanceRepository openingBalances;
    @Mock BalanceNotifier notifier;
    private final UUID spaceId = UUID.randomUUID();

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_removes_it() {
        new ClearOpeningBalanceHandler(openingBalances, notifier).clear(spaceId, membership(SpaceRole.MEMBER));

        verify(openingBalances).deleteBySpaceId(spaceId);
    }

    @Test
    void a_viewer_cannot_remove_it() {
        assertThatThrownBy(() -> new ClearOpeningBalanceHandler(openingBalances, notifier).clear(spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(openingBalances, never()).deleteBySpaceId(any());
    }

    @Test
    void the_notifier_is_told_the_opening_balance_read_before_it_goes() {
        OpeningBalance before = new OpeningBalance(spaceId, new BigDecimal("1520.30"), LocalDate.of(2026, 10, 1));
        when(openingBalances.findBySpaceId(spaceId)).thenReturn(Optional.of(before));
        SpaceMembership caller = membership(SpaceRole.MEMBER);

        new ClearOpeningBalanceHandler(openingBalances, notifier).clear(spaceId, caller);

        InOrder order = inOrder(openingBalances, notifier);
        order.verify(openingBalances).findBySpaceId(spaceId);
        order.verify(openingBalances).deleteBySpaceId(spaceId);
        order.verify(notifier).openingBalanceCleared(caller, Optional.of(before));
    }

    @Test
    void a_refused_gesture_tells_nobody() {
        assertThatThrownBy(() -> new ClearOpeningBalanceHandler(openingBalances, notifier).clear(spaceId, membership(SpaceRole.VIEWER)));
        verifyNoInteractions(notifier);
    }
}
