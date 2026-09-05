package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSavingsGoalHandlerTest {

    @Mock SavingsGoalRepository savingsGoalRepository;
    private UpdateSavingsGoalHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateSavingsGoalHandler(savingsGoalRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private SavingsGoal existingInSameSpace(UUID goalId) {
        return new SavingsGoal(goalId, spaceId, "Vacances", new BigDecimal("2000.00"), null);
    }

    @Test
    void a_member_can_update_a_savings_goal() {
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(UUID.randomUUID(), spaceId, "Nouveau nom", new BigDecimal("2500.00"), null);
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(existingInSameSpace(command.goalId())));
        SavingsGoal updated = new SavingsGoal(command.goalId(), spaceId, "Nouveau nom", new BigDecimal("2500.00"), null);
        when(savingsGoalRepository.update(command)).thenReturn(updated);

        SavingsGoal result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void updating_a_savings_goal_that_belongs_to_a_different_space_is_rejected() {
        UUID goalId = UUID.randomUUID();
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(goalId, spaceId, "Nouveau nom", new BigDecimal("2500.00"), null);
        SavingsGoal foreign = new SavingsGoal(goalId, UUID.randomUUID(), "Vacances", new BigDecimal("2000.00"), null);
        when(savingsGoalRepository.findById(goalId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.SavingsGoalNotFound.class);
        verify(savingsGoalRepository, never()).update(any());
    }

    @Test
    void updating_a_savings_goal_that_does_not_exist_is_rejected() {
        UUID goalId = UUID.randomUUID();
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(goalId, spaceId, "Nouveau nom", new BigDecimal("2500.00"), null);
        when(savingsGoalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.SavingsGoalNotFound.class);
    }

    @Test
    void a_viewer_cannot_update_a_savings_goal() {
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(UUID.randomUUID(), spaceId, "Nouveau nom", new BigDecimal("2500.00"), null);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
