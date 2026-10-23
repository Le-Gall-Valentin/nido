package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.service.SavingsNotifier;
import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSavingsGoalHandlerTest {

    @Mock SavingsGoalRepository savingsGoalRepository;
    @Mock SavingsNotifier notifier;
    private CreateSavingsGoalHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateSavingsGoalHandler(savingsGoalRepository, notifier);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_create_a_savings_goal() {
        CreateSavingsGoalCommand command = new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        SavingsGoal created = new SavingsGoal(UUID.randomUUID(), spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.create(command)).thenReturn(created);

        SavingsGoal result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_viewer_cannot_create_a_savings_goal() {
        CreateSavingsGoalCommand command = new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_created_goal_tells_the_notifier_after_writing() {
        CreateSavingsGoalCommand command = new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        SavingsGoal created = new SavingsGoal(UUID.randomUUID(), spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.create(command)).thenReturn(created);
        SpaceMembership caller = membership(SpaceRole.MEMBER);

        handler.create(command, caller);

        InOrder order = inOrder(savingsGoalRepository, notifier);
        order.verify(savingsGoalRepository).create(command);
        order.verify(notifier).goalCreated(caller, created);
    }

    @Test
    void a_refused_gesture_tells_nobody() {
        CreateSavingsGoalCommand command = new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)));
        verifyNoInteractions(notifier);
    }
}
