package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsContribution;
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
class AddSavingsContributionHandlerTest {

    @Mock SavingsGoalRepository savingsGoalRepository;
    private AddSavingsContributionHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new AddSavingsContributionHandler(savingsGoalRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_add_a_contribution() {
        AddSavingsContributionCommand command = new AddSavingsContributionCommand(UUID.randomUUID(), spaceId, UUID.randomUUID(), new BigDecimal("100.00"), LocalDate.of(2026, 1, 5));
        SavingsGoal goal = new SavingsGoal(command.goalId(), spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(goal));
        when(savingsGoalRepository.findContributionsByGoalId(command.goalId())).thenReturn(List.of());
        SavingsContribution created = new SavingsContribution(UUID.randomUUID(), command.goalId(), command.memberId(), command.amount(), command.date());
        when(savingsGoalRepository.addContribution(command)).thenReturn(created);

        SavingsContribution result = handler.add(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_contribution_exactly_reaching_the_target_is_accepted() {
        AddSavingsContributionCommand command = new AddSavingsContributionCommand(UUID.randomUUID(), spaceId, UUID.randomUUID(), new BigDecimal("500.00"), LocalDate.of(2026, 1, 5));
        SavingsGoal goal = new SavingsGoal(command.goalId(), spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(goal));
        SavingsContribution existing = new SavingsContribution(UUID.randomUUID(), command.goalId(), command.memberId(), new BigDecimal("1500.00"), LocalDate.of(2026, 1, 1));
        when(savingsGoalRepository.findContributionsByGoalId(command.goalId())).thenReturn(List.of(existing));
        SavingsContribution created = new SavingsContribution(UUID.randomUUID(), command.goalId(), command.memberId(), command.amount(), command.date());
        when(savingsGoalRepository.addContribution(command)).thenReturn(created);

        SavingsContribution result = handler.add(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_contribution_that_would_exceed_the_goal_target_is_rejected() {
        AddSavingsContributionCommand command = new AddSavingsContributionCommand(UUID.randomUUID(), spaceId, UUID.randomUUID(), new BigDecimal("500.01"), LocalDate.of(2026, 1, 5));
        SavingsGoal goal = new SavingsGoal(command.goalId(), spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(goal));
        SavingsContribution existing = new SavingsContribution(UUID.randomUUID(), command.goalId(), command.memberId(), new BigDecimal("1500.00"), LocalDate.of(2026, 1, 1));
        when(savingsGoalRepository.findContributionsByGoalId(command.goalId())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> handler.add(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.ContributionExceedsGoalTarget.class);
        verify(savingsGoalRepository, never()).addContribution(any());
    }

    @Test
    void adding_a_contribution_to_a_goal_that_belongs_to_a_different_space_is_rejected() {
        AddSavingsContributionCommand command = new AddSavingsContributionCommand(UUID.randomUUID(), spaceId, UUID.randomUUID(), new BigDecimal("100.00"), LocalDate.of(2026, 1, 5));
        SavingsGoal foreign = new SavingsGoal(command.goalId(), UUID.randomUUID(), "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> handler.add(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.SavingsGoalNotFound.class);
    }

    @Test
    void adding_a_contribution_to_a_goal_that_does_not_exist_is_rejected() {
        AddSavingsContributionCommand command = new AddSavingsContributionCommand(UUID.randomUUID(), spaceId, UUID.randomUUID(), new BigDecimal("100.00"), LocalDate.of(2026, 1, 5));
        when(savingsGoalRepository.findById(command.goalId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.add(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.SavingsGoalNotFound.class);
    }

    @Test
    void a_viewer_cannot_add_a_contribution() {
        AddSavingsContributionCommand command = new AddSavingsContributionCommand(UUID.randomUUID(), spaceId, UUID.randomUUID(), new BigDecimal("100.00"), LocalDate.of(2026, 1, 5));

        assertThatThrownBy(() -> handler.add(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
