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
    void goalsComeMostPressingFirst() {
        when(listGoals.list(caller)).thenReturn(List.of(
            goal("Canapé", "900.00", "900.00", TODAY.plusDays(90)),
            goal("Vélo", "1000.00", "100.00", null),
            goal("Vacances", "3000.00", "1450.00", LocalDate.of(2027, 6, 30)),
            goal("Anniversaire", "200.00", "50.00", TODAY.plusDays(20))));

        assertThat(card().goals()).extracting(SavingsGoalItem::name, SavingsGoalItem::state)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Anniversaire", GoalState.DUE_SOON),
                org.assertj.core.groups.Tuple.tuple("Vacances", GoalState.IN_PROGRESS),
                org.assertj.core.groups.Tuple.tuple("Vélo", GoalState.IN_PROGRESS),
                org.assertj.core.groups.Tuple.tuple("Canapé", GoalState.REACHED));
    }

    @Test
    void aGoalWhoseDateHasPassedComesFirst() {
        when(listGoals.list(caller)).thenReturn(List.of(
            goal("Anniversaire", "200.00", "50.00", TODAY.plusDays(20)),
            goal("Voiture", "5000.00", "4000.00", TODAY.minusDays(3))));

        assertThat(card().goals()).extracting(SavingsGoalItem::state)
            .containsExactly(GoalState.PAST_DUE, GoalState.DUE_SOON);
    }

    @Test
    void dueSoonMeansAtMostThirtyDaysAway() {
        when(listGoals.list(caller)).thenReturn(List.of(
            goal("À trente jours", "100.00", "0.00", TODAY.plusDays(30)),
            goal("À trente et un jours", "100.00", "0.00", TODAY.plusDays(31))));

        assertThat(card().goals()).extracting(SavingsGoalItem::name, SavingsGoalItem::state)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("À trente jours", GoalState.DUE_SOON),
                org.assertj.core.groups.Tuple.tuple("À trente et un jours", GoalState.IN_PROGRESS));
    }

    @Test
    void theMonthlyAmountSpreadsTheRemainderOverTheMonthsLeftRoundedUp() {
        SavingsGoalDetail holidays = goal("Vacances", "3000.00", "1450.00", LocalDate.of(2027, 6, 30));
        when(listGoals.list(caller)).thenReturn(List.of(holidays));

        SavingsGoalItem item = card().goals().getFirst();

        assertThat(item).isEqualTo(new SavingsGoalItem(holidays.goal().id(), "Vacances", "🎯", "#44618a",
            new BigDecimal("3000.00"), new BigDecimal("1450.00"), LocalDate.of(2027, 6, 30),
            GoalState.IN_PROGRESS, new BigDecimal("172.23")));
    }

    @Test
    void aTargetInTheCurrentMonthOrAlreadyPastAsksForTheWholeRemainder() {
        when(listGoals.list(caller)).thenReturn(List.of(
            goal("Ce mois-ci", "500.00", "200.00", LocalDate.of(2026, 9, 30)),
            goal("Dépassé", "800.00", "100.00", LocalDate.of(2026, 8, 1))));

        assertThat(card().goals()).extracting(SavingsGoalItem::name, SavingsGoalItem::monthlyNeeded)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Dépassé", new BigDecimal("700.00")),
                org.assertj.core.groups.Tuple.tuple("Ce mois-ci", new BigDecimal("300.00")));
    }

    @Test
    void noTargetDateOrAReachedGoalAsksForNoMonthlyAmount() {
        when(listGoals.list(caller)).thenReturn(List.of(
            goal("Vélo", "1000.00", "100.00", null),
            goal("Canapé", "900.00", "950.00", TODAY.plusDays(90))));

        assertThat(card().goals()).extracting(SavingsGoalItem::monthlyNeeded).containsOnlyNulls();
    }

    @Test
    void keepsAtMostFourGoals() {
        when(listGoals.list(caller)).thenReturn(List.of(
            goal("A", "100", "0", TODAY.plusDays(100)), goal("B", "100", "0", TODAY.plusDays(101)),
            goal("C", "100", "0", TODAY.plusDays(102)), goal("D", "100", "0", TODAY.plusDays(103)),
            goal("E", "100", "0", TODAY.plusDays(104))));

        assertThat(card().goals()).extracting(SavingsGoalItem::name).containsExactly("A", "B", "C", "D");
    }

    @Test
    void noGoalMeansNoCard() {
        when(listGoals.list(caller)).thenReturn(List.of());

        assertThat(read(SpaceType.SHARED).card()).isNull();
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

    /** One contribution carrying the whole amount already saved. */
    private SavingsGoalDetail goal(String name, String target, String contributed, LocalDate targetDate) {
        UUID goalId = UUID.randomUUID();
        return new SavingsGoalDetail(
            new SavingsGoal(goalId, spaceId, name, new BigDecimal(target), targetDate, "#44618a", "🎯"),
            List.of(new SavingsContribution(UUID.randomUUID(), goalId, caller.userId(), new BigDecimal(contributed), TODAY)));
    }
}
