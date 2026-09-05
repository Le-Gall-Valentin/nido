package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListSavingsGoalsHandlerTest {

    @Mock SavingsGoalRepository savingsGoalRepository;
    private ListSavingsGoalsHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListSavingsGoalsHandler(savingsGoalRepository);
    }

    @Test
    void combines_each_goal_with_its_contributions() {
        SavingsGoal goal = new SavingsGoal(UUID.randomUUID(), spaceId, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🎯");
        when(savingsGoalRepository.findBySpaceId(spaceId)).thenReturn(List.of(goal));
        when(savingsGoalRepository.findContributionsByGoalId(goal.id())).thenReturn(List.of());
        SpaceMembership membership = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        List<SavingsGoalDetail> result = handler.list(membership);

        assertThat(result).containsExactly(new SavingsGoalDetail(goal, List.of()));
    }
}
