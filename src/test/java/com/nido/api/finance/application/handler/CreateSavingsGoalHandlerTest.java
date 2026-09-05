package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSavingsGoalHandlerTest {

    @Mock SavingsGoalRepository savingsGoalRepository;
    private CreateSavingsGoalHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateSavingsGoalHandler(savingsGoalRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_create_a_savings_goal() {
        CreateSavingsGoalCommand command = new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null);
        SavingsGoal created = new SavingsGoal(UUID.randomUUID(), spaceId, "Vacances", new BigDecimal("2000.00"), null);
        when(savingsGoalRepository.create(command)).thenReturn(created);

        SavingsGoal result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_viewer_cannot_create_a_savings_goal() {
        CreateSavingsGoalCommand command = new CreateSavingsGoalCommand(spaceId, "Vacances", new BigDecimal("2000.00"), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
