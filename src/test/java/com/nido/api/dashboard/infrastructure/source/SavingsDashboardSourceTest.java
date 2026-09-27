package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.GoalState;
import com.nido.api.dashboard.domain.model.SavingsCard;
import com.nido.api.dashboard.domain.model.SavingsGoalItem;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.finance.application.port.in.ListSavingsGoalsUseCase;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SavingsDashboardSourceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    private final ListSavingsGoalsUseCase listGoals = mock(ListSavingsGoalsUseCase.class);

    @Test
    void aGoalReachesTheCardWithEverythingPutTowardIt() {
        // The states, the monthly amount and the order are SavingsOutlook's (see SavingsOutlookTest).
        UUID goalId = UUID.randomUUID();
        when(listGoals.list(caller)).thenReturn(List.of(new SavingsGoalDetail(
            new SavingsGoal(goalId, spaceId, "Vacances", new BigDecimal("3000.00"), LocalDate.of(2027, 6, 30), "#44618a", "🎯"),
            List.of(
                new SavingsContribution(UUID.randomUUID(), goalId, caller.userId(), new BigDecimal("1000.00"), TODAY),
                new SavingsContribution(UUID.randomUUID(), goalId, caller.userId(), new BigDecimal("450.00"), TODAY)))));

        assertThat(card().goals()).containsExactly(new SavingsGoalItem(goalId, "Vacances", "🎯", "#44618a",
            new BigDecimal("3000.00"), new BigDecimal("1450.00"), LocalDate.of(2027, 6, 30),
            GoalState.IN_PROGRESS, new BigDecimal("172.23")));
    }

    @Test
    void aPersonalSpaceShowsNoSavingsAndNeverAsks() {
        SourceResult result = read(SpaceType.PERSONAL);

        assertThat(result.card()).isNull();
        verifyNoInteractions(listGoals);
    }

    @Test
    void itIsTheSavingsSource() {
        assertThat(new SavingsDashboardSource(listGoals).kind()).isEqualTo(CardKind.SAVINGS);
    }

    private SavingsCard card() {
        return (SavingsCard) read(SpaceType.SHARED).card();
    }

    private SourceResult read(SpaceType type) {
        return new SavingsDashboardSource(listGoals).read(new DashboardContext(caller, "me@test.com", TODAY, type));
    }
}
