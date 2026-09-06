package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteSavingsGoalHandlerTest {

    @Mock SavingsGoalRepository savingsGoalRepository;
    private DeleteSavingsGoalHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID goalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteSavingsGoalHandler(savingsGoalRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_delete_a_savings_goal() {
        SavingsGoal existing = new SavingsGoal(goalId, spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(goalId)).thenReturn(Optional.of(existing));

        handler.delete(goalId, spaceId, membership(SpaceRole.MEMBER));

        verify(savingsGoalRepository).delete(goalId);
    }

    @Test
    void deleting_a_goal_belonging_to_another_space_is_rejected() {
        SavingsGoal existing = new SavingsGoal(goalId, UUID.randomUUID(), "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(goalId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> handler.delete(goalId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.SavingsGoalNotFound.class);
    }

    @Test
    void a_viewer_cannot_delete_a_savings_goal() {
        assertThatThrownBy(() -> handler.delete(goalId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
