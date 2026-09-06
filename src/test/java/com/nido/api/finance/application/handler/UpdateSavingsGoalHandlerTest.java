package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
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
import java.time.LocalDate;
import java.util.List;
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
        return new SavingsGoal(goalId, spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
    }

    @Test
    void a_member_can_update_a_savings_goal() {
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(UUID.randomUUID(), spaceId, "Nouveau nom", new BigDecimal("2500.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(existingInSameSpace(command.goalId())));
        SavingsGoal updated = new SavingsGoal(command.goalId(), spaceId, "Nouveau nom", new BigDecimal("2500.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.update(command)).thenReturn(updated);
        when(savingsGoalRepository.findContributionsByGoalId(command.goalId())).thenReturn(List.of());

        SavingsGoalDetail result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result.goal()).isEqualTo(updated);
    }

    @Test
    void updating_a_savings_goal_returns_its_actual_existing_contributions_instead_of_an_empty_list() {
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(UUID.randomUUID(), spaceId, "Nouveau nom", new BigDecimal("2500.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(existingInSameSpace(command.goalId())));
        SavingsGoal updated = new SavingsGoal(command.goalId(), spaceId, "Nouveau nom", new BigDecimal("2500.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.update(command)).thenReturn(updated);
        SavingsContribution contribution = new SavingsContribution(UUID.randomUUID(), command.goalId(), UUID.randomUUID(), new BigDecimal("100.00"), LocalDate.of(2026, 1, 5));
        when(savingsGoalRepository.findContributionsByGoalId(command.goalId())).thenReturn(List.of(contribution));

        SavingsGoalDetail result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result.contributions()).containsExactly(contribution);
    }

    @Test
    void lowering_the_target_amount_below_what_has_already_been_contributed_is_rejected() {
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(UUID.randomUUID(), spaceId, "Nouveau nom", new BigDecimal("500.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(existingInSameSpace(command.goalId())));
        SavingsContribution contribution = new SavingsContribution(UUID.randomUUID(), command.goalId(), UUID.randomUUID(), new BigDecimal("600.00"), LocalDate.of(2026, 1, 5));
        when(savingsGoalRepository.findContributionsByGoalId(command.goalId())).thenReturn(List.of(contribution));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.TargetAmountBelowContributed.class);
        verify(savingsGoalRepository, never()).update(any());
    }

    @Test
    void lowering_the_target_amount_to_exactly_what_has_already_been_contributed_is_accepted() {
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(UUID.randomUUID(), spaceId, "Nouveau nom", new BigDecimal("600.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(existingInSameSpace(command.goalId())));
        SavingsContribution contribution = new SavingsContribution(UUID.randomUUID(), command.goalId(), UUID.randomUUID(), new BigDecimal("600.00"), LocalDate.of(2026, 1, 5));
        when(savingsGoalRepository.findContributionsByGoalId(command.goalId())).thenReturn(List.of(contribution));
        SavingsGoal updated = new SavingsGoal(command.goalId(), spaceId, "Nouveau nom", new BigDecimal("600.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.update(command)).thenReturn(updated);

        SavingsGoalDetail result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result.goal()).isEqualTo(updated);
    }

    @Test
    void updating_a_savings_goal_that_belongs_to_a_different_space_is_rejected() {
        UUID goalId = UUID.randomUUID();
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(goalId, spaceId, "Nouveau nom", new BigDecimal("2500.00"), null, "#5c7a58", "🎯");
        SavingsGoal foreign = new SavingsGoal(goalId, UUID.randomUUID(), "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(goalId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.SavingsGoalNotFound.class);
        verify(savingsGoalRepository, never()).update(any());
    }

    @Test
    void updating_a_savings_goal_that_does_not_exist_is_rejected() {
        UUID goalId = UUID.randomUUID();
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(goalId, spaceId, "Nouveau nom", new BigDecimal("2500.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(goalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.SavingsGoalNotFound.class);
    }

    @Test
    void a_viewer_cannot_update_a_savings_goal() {
        UpdateSavingsGoalCommand command = new UpdateSavingsGoalCommand(UUID.randomUUID(), spaceId, "Nouveau nom", new BigDecimal("2500.00"), null, "#5c7a58", "🎯");

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
