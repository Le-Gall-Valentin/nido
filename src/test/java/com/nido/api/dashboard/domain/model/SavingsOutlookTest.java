package com.nido.api.dashboard.domain.model;

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
import static org.assertj.core.groups.Tuple.tuple;

class SavingsOutlookTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        SpaceRole.MEMBER, Instant.now());

    @Test
    void goalsComeMostPressingFirst() {
        assertThat(card(
            goal("Canapé", "900.00", "900.00", TODAY.plusDays(90)),
            goal("Vélo", "1000.00", "100.00", null),
            goal("Vacances", "3000.00", "1450.00", LocalDate.of(2027, 6, 30)),
            goal("Anniversaire", "200.00", "50.00", TODAY.plusDays(20))).goals())
            .extracting(SavingsGoalItem::name, SavingsGoalItem::state)
            .containsExactly(
                tuple("Anniversaire", GoalState.DUE_SOON),
                tuple("Vacances", GoalState.IN_PROGRESS),
                tuple("Vélo", GoalState.IN_PROGRESS),
                tuple("Canapé", GoalState.REACHED));
    }

    @Test
    void aGoalWhoseDateHasPassedComesFirst() {
        assertThat(card(
            goal("Anniversaire", "200.00", "50.00", TODAY.plusDays(20)),
            goal("Voiture", "5000.00", "4000.00", TODAY.minusDays(3))).goals())
            .extracting(SavingsGoalItem::state)
            .containsExactly(GoalState.PAST_DUE, GoalState.DUE_SOON);
    }

    @Test
    void dueSoonMeansAtMostThirtyDaysAway() {
        assertThat(card(
            goal("À trente jours", "100.00", "0.00", TODAY.plusDays(SavingsOutlook.DUE_SOON_DAYS)),
            goal("À trente et un jours", "100.00", "0.00", TODAY.plusDays(SavingsOutlook.DUE_SOON_DAYS + 1))).goals())
            .extracting(SavingsGoalItem::name, SavingsGoalItem::state)
            .containsExactly(
                tuple("À trente jours", GoalState.DUE_SOON),
                tuple("À trente et un jours", GoalState.IN_PROGRESS));
    }

    @Test
    void theMonthlyAmountSpreadsTheRemainderOverTheMonthsLeftRoundedUp() {
        SavingsOutlook.Goal holidays = goal("Vacances", "3000.00", "1450.00", LocalDate.of(2027, 6, 30));

        assertThat(card(holidays).goals().getFirst()).isEqualTo(new SavingsGoalItem(holidays.goalId(), "Vacances", "🎯",
            "#44618a", new BigDecimal("3000.00"), new BigDecimal("1450.00"), LocalDate.of(2027, 6, 30),
            GoalState.IN_PROGRESS, new BigDecimal("172.23")));
    }

    @Test
    void aTargetInTheCurrentMonthOrAlreadyPastAsksForTheWholeRemainder() {
        assertThat(card(
            goal("Ce mois-ci", "500.00", "200.00", LocalDate.of(2026, 9, 30)),
            goal("Dépassé", "800.00", "100.00", LocalDate.of(2026, 8, 1))).goals())
            .extracting(SavingsGoalItem::name, SavingsGoalItem::monthlyNeeded)
            .containsExactly(
                tuple("Dépassé", new BigDecimal("700.00")),
                tuple("Ce mois-ci", new BigDecimal("300.00")));
    }

    @Test
    void noTargetDateOrAReachedGoalAsksForNoMonthlyAmount() {
        assertThat(card(
            goal("Vélo", "1000.00", "100.00", null),
            goal("Canapé", "900.00", "950.00", TODAY.plusDays(90))).goals())
            .extracting(SavingsGoalItem::monthlyNeeded).containsOnlyNulls();
    }

    @Test
    void keepsTheMostPressingGoalsOnly() {
        assertThat(card(
            goal("A", "100", "0", TODAY.plusDays(100)), goal("B", "100", "0", TODAY.plusDays(101)),
            goal("C", "100", "0", TODAY.plusDays(102)), goal("D", "100", "0", TODAY.plusDays(103)),
            goal("E", "100", "0", TODAY.plusDays(104))).goals())
            .extracting(SavingsGoalItem::name).containsExactly("A", "B", "C", "D");
    }

    @Test
    void noGoalMeansNoCard() {
        assertThat(SavingsOutlook.of(List.of(), context(SpaceType.SHARED)).card()).isNull();
    }

    @Test
    void onlyASharedSpaceHasSavings() {
        SourceResult personal = SavingsOutlook.of(List.of(goal("Vélo", "1000.00", "100.00", null)), context(SpaceType.PERSONAL));

        assertThat(SavingsOutlook.hasSavings(context(SpaceType.SHARED))).isTrue();
        assertThat(SavingsOutlook.hasSavings(context(SpaceType.PERSONAL))).isFalse();
        assertThat(personal.card()).isNull();
    }

    private SavingsCard card(SavingsOutlook.Goal... goals) {
        return (SavingsCard) SavingsOutlook.of(List.of(goals), context(SpaceType.SHARED)).card();
    }

    private DashboardContext context(SpaceType type) {
        return new DashboardContext(caller, TODAY, type);
    }

    private static SavingsOutlook.Goal goal(String name, String target, String contributed, LocalDate targetDate) {
        return new SavingsOutlook.Goal(UUID.randomUUID(), name, "🎯", "#44618a", new BigDecimal(target),
            new BigDecimal(contributed), targetDate);
    }
}
