package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.service.SavingsNotifier;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteSavingsGoalHandlerTest {

    @Mock SavingsGoalRepository savingsGoalRepository;
    @Mock SavingsNotifier notifier;
    private DeleteSavingsGoalHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID goalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteSavingsGoalHandler(savingsGoalRepository, notifier);
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

    @Test
    void deleting_a_goal_reads_its_deposits_before_they_go() {
        SavingsGoal existing = new SavingsGoal(goalId, spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        SavingsContribution deposit = new SavingsContribution(UUID.randomUUID(), goalId, UUID.randomUUID(), new BigDecimal("300.00"), LocalDate.of(2026, 1, 1));
        when(savingsGoalRepository.findById(goalId)).thenReturn(Optional.of(existing));
        when(savingsGoalRepository.findContributionsByGoalId(goalId)).thenReturn(List.of(deposit));
        SpaceMembership caller = membership(SpaceRole.MEMBER);

        handler.delete(goalId, spaceId, caller);

        InOrder order = inOrder(savingsGoalRepository, notifier);
        order.verify(savingsGoalRepository).findContributionsByGoalId(goalId);
        order.verify(savingsGoalRepository).delete(goalId);
        order.verify(notifier).goalDeleted(caller, existing, List.of(deposit));
    }

    @Test
    void a_refused_gesture_tells_nobody() {
        assertThatThrownBy(() -> handler.delete(goalId, spaceId, membership(SpaceRole.VIEWER)));
        verifyNoInteractions(notifier);
    }
}
