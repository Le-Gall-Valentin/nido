package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceReviewTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final FinanceReview.Figures FIGURES = new FinanceReview.Figures(YearMonth.of(2026, 9),
        new BigDecimal("1240.00"), new BigDecimal("1860.00"), new BigDecimal("3100.00"), new BigDecimal("340.00"));
    private final UUID me = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), me, SpaceRole.MEMBER, Instant.now());

    private final BudgetWatch restaurantsOver = budget("Restaurants", "180.00", "212.00", BudgetWatch.Status.OVER);
    private final BudgetWatch groceriesWarning = budget("Courses alimentaires", "500.00", "430.00", BudgetWatch.Status.WARNING);

    @Test
    void theCardCarriesTheMonthFigures() {
        FinanceCard card = card(review(List.of(), List.of(), List.of(), SpaceType.SHARED));

        assertThat(card.month()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(card.balance()).isEqualByComparingTo("1240.00");
        assertThat(card.totalExpense()).isEqualByComparingTo("1860.00");
        assertThat(card.totalIncome()).isEqualByComparingTo("3100.00");
        assertThat(card.remainingBudget()).isEqualByComparingTo("340.00");
    }

    @Test
    void budgetsToWatchComeMostConsumedFirst() {
        FinanceCard card = card(review(List.of(groceriesWarning, restaurantsOver), List.of(), List.of(), SpaceType.SHARED));

        assertThat(card.budgetsToWatch()).containsExactly(restaurantsOver, groceriesWarning);
    }

    @Test
    void onlyOverBudgetsRaiseAnOverrun() {
        SourceResult result = review(List.of(groceriesWarning, restaurantsOver), List.of(), List.of(), SpaceType.SHARED);

        assertThat(result.attention()).containsExactly(new AttentionItem.BudgetOverrun(restaurantsOver.categoryId(),
            "Restaurants", new BigDecimal("212.00"), new BigDecimal("180.00")));
    }

    @Test
    void aZeroEuroBudgetWithSpendingOutranksEveryOtherLine() {
        // "Spend nothing here": the first euro is an overrun, and no ratio can top it.
        BudgetWatch gifts = budget("Cadeaux", "0.00", "5.00", BudgetWatch.Status.OVER);
        BudgetWatch restaurants = budget("Restaurants", "180.00", "400.00", BudgetWatch.Status.OVER);

        SourceResult result = review(List.of(restaurants, gifts), List.of(), List.of(), SpaceType.SHARED);

        assertThat(card(result).budgetsToWatch()).extracting(BudgetWatch::label).containsExactly("Cadeaux", "Restaurants");
        assertThat(result.attention()).extracting(item -> ((AttentionItem.BudgetOverrun) item).label())
            .containsExactly("Cadeaux", "Restaurants");
    }

    @Test
    void upcomingComesByDateThenLabelAndKeepsTheFiveEarliest() {
        List<UpcomingOperation> operations = new ArrayList<>();
        for (int day = 7; day >= 1; day--) {
            operations.add(operation("J+" + day, TODAY.plusDays(day)));
        }
        operations.add(operation("A — same day as J+1", TODAY.plusDays(1)));

        assertThat(card(review(List.of(), operations, List.of(), SpaceType.SHARED)).upcoming())
            .extracting(UpcomingOperation::label)
            .containsExactly("A — same day as J+1", "J+1", "J+2", "J+3", "J+4");
    }

    @Test
    void balancesAreSeenFromTheCallersSideAndRaiseADebtPerCreditor() {
        UUID camille = UUID.randomUUID();
        UUID paul = UUID.randomUUID();
        UUID lea = UUID.randomUUID();
        List<FinanceReview.Transfer> transfers = List.of(
            new FinanceReview.Transfer(paul, me, new BigDecimal("18.00")),
            new FinanceReview.Transfer(me, camille, new BigDecimal("42.50")),
            new FinanceReview.Transfer(lea, paul, new BigDecimal("10.00")),
            new FinanceReview.Transfer(me, lea, new BigDecimal("60.00")));

        SourceResult result = review(List.of(), List.of(), transfers, SpaceType.SHARED);

        assertThat(card(result).balances()).containsExactly(
            new BalanceWithMember(lea, new BigDecimal("60.00"), BalanceDirection.I_OWE),
            new BalanceWithMember(camille, new BigDecimal("42.50"), BalanceDirection.I_OWE),
            new BalanceWithMember(paul, new BigDecimal("18.00"), BalanceDirection.OWES_ME));
        assertThat(result.attention()).containsExactly(
            new AttentionItem.Debt(lea, new BigDecimal("60.00")),
            new AttentionItem.Debt(camille, new BigDecimal("42.50")));
    }

    @Test
    void onlyASharedSpaceHasBalances() {
        List<FinanceReview.Transfer> transfers = List.of(new FinanceReview.Transfer(me, UUID.randomUUID(), BigDecimal.TEN));

        SourceResult personal = review(List.of(), List.of(), transfers, SpaceType.PERSONAL);

        assertThat(FinanceReview.hasBalances(context(SpaceType.SHARED))).isTrue();
        assertThat(FinanceReview.hasBalances(context(SpaceType.PERSONAL))).isFalse();
        assertThat(card(personal).balances()).isNull();
        assertThat(personal.attention()).isEmpty();
    }

    private SourceResult review(List<BudgetWatch> budgets, List<UpcomingOperation> upcoming,
                                List<FinanceReview.Transfer> transfers, SpaceType type) {
        return FinanceReview.of(FIGURES, budgets, upcoming, transfers, context(type));
    }

    private DashboardContext context(SpaceType type) {
        return new DashboardContext(caller, TODAY, type);
    }

    private static FinanceCard card(SourceResult result) {
        return (FinanceCard) result.card();
    }

    private static BudgetWatch budget(String label, String limit, String spent, BudgetWatch.Status status) {
        return new BudgetWatch(UUID.randomUUID(), label, "#a3463a", new BigDecimal(spent), new BigDecimal(limit), status);
    }

    private static UpcomingOperation operation(String label, LocalDate date) {
        return new UpcomingOperation(date, label, BigDecimal.ONE, UpcomingOperation.Type.EXPENSE, UUID.randomUUID());
    }
}
