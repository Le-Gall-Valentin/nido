package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.BalanceDirection;
import com.nido.api.dashboard.domain.model.BalanceWithMember;
import com.nido.api.dashboard.domain.model.BudgetWatch;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.FinanceCard;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.model.UpcomingOperation;
import com.nido.api.finance.application.port.in.GetBalancesUseCase;
import com.nido.api.finance.application.port.in.GetFinanceStatsUseCase;
import com.nido.api.finance.application.port.in.ListCategoriesUseCase;
import com.nido.api.finance.application.port.in.ListTransactionsInRangeUseCase;
import com.nido.api.finance.application.port.in.ProjectRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.Balances;
import com.nido.api.finance.domain.model.BudgetLine;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.FinanceStats;
import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.SuggestedTransfer;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FinanceDashboardSourceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private final UUID spaceId = UUID.randomUUID();
    private final UUID me = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, me, SpaceRole.MEMBER, Instant.now());

    private final GetFinanceStatsUseCase getStats = mock(GetFinanceStatsUseCase.class);
    private final ListCategoriesUseCase listCategories = mock(ListCategoriesUseCase.class);
    private final ListTransactionsInRangeUseCase listTransactions = mock(ListTransactionsInRangeUseCase.class);
    private final ProjectRecurringSeriesUseCase projectSeries = mock(ProjectRecurringSeriesUseCase.class);
    private final GetBalancesUseCase getBalances = mock(GetBalancesUseCase.class);

    private final Category restaurants = category("Restaurants", "#a3463a");
    private final Category groceries = category("Courses alimentaires", "#8a6d2e");
    private final Category leisure = category("Loisirs", "#44618a");
    private final Category gifts = category("Cadeaux", "#7b5c93");

    @Test
    void readsTheStatsOfTheSpaceCurrentMonth() {
        stats(List.of());
        noUpcoming(TODAY);
        noBalances();

        FinanceCard card = (FinanceCard) read(TODAY, SpaceType.SHARED).card();

        assertThat(card.month()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(card.balance()).isEqualByComparingTo("1240.00");
        assertThat(card.totalExpense()).isEqualByComparingTo("1860.00");
        assertThat(card.totalIncome()).isEqualByComparingTo("3100.00");
        assertThat(card.remainingBudget()).isEqualByComparingTo("340.00");
    }

    @Test
    void budgetsToWatchKeepWarningAndOverLinesMostConsumedFirst() {
        stats(List.of(
            new BudgetLine(leisure.id(), new BigDecimal("100.00"), new BigDecimal("10.00")),
            new BudgetLine(groceries.id(), new BigDecimal("500.00"), new BigDecimal("430.00")),
            new BudgetLine(restaurants.id(), new BigDecimal("180.00"), new BigDecimal("212.00"))));
        noUpcoming(TODAY);
        noBalances();

        FinanceCard card = (FinanceCard) read(TODAY, SpaceType.SHARED).card();

        assertThat(card.budgetsToWatch()).containsExactly(
            new BudgetWatch(restaurants.id(), "Restaurants", "#a3463a", new BigDecimal("212.00"), new BigDecimal("180.00"), "OVER"),
            new BudgetWatch(groceries.id(), "Courses alimentaires", "#8a6d2e", new BigDecimal("430.00"), new BigDecimal("500.00"), "WARNING"));
    }

    @Test
    void onlyOverBudgetsRaiseAnOverrun() {
        stats(List.of(
            new BudgetLine(groceries.id(), new BigDecimal("500.00"), new BigDecimal("430.00")),
            new BudgetLine(restaurants.id(), new BigDecimal("180.00"), new BigDecimal("212.00"))));
        noUpcoming(TODAY);
        noBalances();

        assertThat(read(TODAY, SpaceType.SHARED).attention()).containsExactly(
            new AttentionItem.BudgetOverrun(restaurants.id(), "Restaurants", new BigDecimal("212.00"), new BigDecimal("180.00")));
    }

    @Test
    void aZeroEuroBudgetIsOverFromTheFirstEuroAndSortsFirst() {
        stats(List.of(
            new BudgetLine(restaurants.id(), new BigDecimal("180.00"), new BigDecimal("400.00")),
            new BudgetLine(gifts.id(), new BigDecimal("0.00"), new BigDecimal("5.00")),
            new BudgetLine(leisure.id(), new BigDecimal("0.00"), new BigDecimal("0.00"))));
        noUpcoming(TODAY);
        noBalances();

        SourceResult result = read(TODAY, SpaceType.SHARED);

        assertThat(((FinanceCard) result.card()).budgetsToWatch()).extracting(BudgetWatch::label)
            .containsExactly("Cadeaux", "Restaurants");
        assertThat(result.attention()).extracting(item -> ((AttentionItem.BudgetOverrun) item).label())
            .containsExactly("Cadeaux", "Restaurants");
    }

    @Test
    void upcomingMergesCreatedAndProjectedOccurrencesAcrossTheMonthEnd() {
        LocalDate today = LocalDate.of(2026, 9, 28);
        stats(List.of());
        UUID rent = UUID.randomUUID();
        UUID netflix = UUID.randomUUID();
        when(listTransactions.list(caller, today.plusDays(1), today.plusDays(7))).thenReturn(List.of(
            transaction("Courses", null, LocalDate.of(2026, 9, 29)),
            transaction("Assurance habitation", UUID.randomUUID(), LocalDate.of(2026, 9, 30))));
        when(projectSeries.project(caller, today.plusDays(1), today.plusDays(7))).thenReturn(List.of(
            new ProjectedOccurrence(netflix, "Netflix", new BigDecimal("13.49"), TransactionType.EXPENSE, LocalDate.of(2026, 10, 3)),
            new ProjectedOccurrence(rent, "Loyer", new BigDecimal("850.00"), TransactionType.EXPENSE, LocalDate.of(2026, 10, 1))));
        noBalances();

        FinanceCard card = (FinanceCard) read(today, SpaceType.SHARED).card();

        assertThat(card.month()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(card.upcoming()).extracting(UpcomingOperation::label, UpcomingOperation::date)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Assurance habitation", LocalDate.of(2026, 9, 30)),
                org.assertj.core.groups.Tuple.tuple("Loyer", LocalDate.of(2026, 10, 1)),
                org.assertj.core.groups.Tuple.tuple("Netflix", LocalDate.of(2026, 10, 3)));
        assertThat(card.upcoming().get(1)).isEqualTo(
            new UpcomingOperation(LocalDate.of(2026, 10, 1), "Loyer", new BigDecimal("850.00"), "EXPENSE", rent));
    }

    @Test
    void upcomingKeepsTheFiveEarliest() {
        stats(List.of());
        when(listTransactions.list(caller, TODAY.plusDays(1), TODAY.plusDays(7))).thenReturn(List.of());
        List<ProjectedOccurrence> seven = new ArrayList<>();
        for (int day = 7; day >= 1; day--) {
            seven.add(new ProjectedOccurrence(UUID.randomUUID(), "J+" + day, BigDecimal.ONE, TransactionType.EXPENSE, TODAY.plusDays(day)));
        }
        when(projectSeries.project(caller, TODAY.plusDays(1), TODAY.plusDays(7))).thenReturn(seven);
        noBalances();

        assertThat(((FinanceCard) read(TODAY, SpaceType.SHARED).card()).upcoming()).extracting(UpcomingOperation::label)
            .containsExactly("J+1", "J+2", "J+3", "J+4", "J+5");
    }

    @Test
    void balancesShowWhatTheCallerOwesAndIsOwedAndRaiseADebtPerCreditor() {
        stats(List.of());
        noUpcoming(TODAY);
        UUID camille = UUID.randomUUID();
        UUID paul = UUID.randomUUID();
        UUID lea = UUID.randomUUID();
        when(getBalances.getBalances(caller)).thenReturn(new Balances(List.of(), List.of(
            new SuggestedTransfer(paul, me, new BigDecimal("18.00")),
            new SuggestedTransfer(me, camille, new BigDecimal("42.50")),
            new SuggestedTransfer(lea, paul, new BigDecimal("10.00")),
            new SuggestedTransfer(me, lea, new BigDecimal("60.00")))));

        SourceResult result = read(TODAY, SpaceType.SHARED);

        assertThat(((FinanceCard) result.card()).balances()).containsExactly(
            new BalanceWithMember(lea, new BigDecimal("60.00"), BalanceDirection.I_OWE),
            new BalanceWithMember(camille, new BigDecimal("42.50"), BalanceDirection.I_OWE),
            new BalanceWithMember(paul, new BigDecimal("18.00"), BalanceDirection.OWES_ME));
        assertThat(result.attention()).containsExactly(
            new AttentionItem.Debt(lea, new BigDecimal("60.00")),
            new AttentionItem.Debt(camille, new BigDecimal("42.50")));
    }

    @Test
    void aPersonalSpaceHasNoBalancesAndNeverAsksForThem() {
        stats(List.of());
        noUpcoming(TODAY);

        FinanceCard card = (FinanceCard) read(TODAY, SpaceType.PERSONAL).card();

        assertThat(card.balances()).isNull();
        verifyNoInteractions(getBalances);
    }

    @Test
    void itIsTheFinanceSource() {
        assertThat(source().kind()).isEqualTo(CardKind.FINANCE);
    }

    private SourceResult read(LocalDate today, SpaceType type) {
        return source().read(new DashboardContext(caller, "me@test.com", today, type));
    }

    private FinanceDashboardSource source() {
        return new FinanceDashboardSource(getStats, listCategories, listTransactions, projectSeries, getBalances);
    }

    private void stats(List<BudgetLine> lines) {
        when(getStats.getStats(YearMonth.of(2026, 9), caller)).thenReturn(new FinanceStats(
            new BigDecimal("1240.00"), new BigDecimal("1860.00"), new BigDecimal("3100.00"), new BigDecimal("340.00"),
            List.of(), lines));
        when(listCategories.list(caller)).thenReturn(List.of(restaurants, groceries, leisure, gifts));
    }

    private void noUpcoming(LocalDate today) {
        when(listTransactions.list(caller, today.plusDays(1), today.plusDays(7))).thenReturn(List.of());
        when(projectSeries.project(caller, today.plusDays(1), today.plusDays(7))).thenReturn(List.of());
    }

    private void noBalances() {
        when(getBalances.getBalances(caller)).thenReturn(new Balances(List.of(), List.of()));
    }

    private Category category(String label, String color) {
        return new Category(UUID.randomUUID(), spaceId, label, color, "tag", false, TransactionType.EXPENSE);
    }

    private Transaction transaction(String label, UUID seriesId, LocalDate date) {
        return new Transaction(UUID.randomUUID(), spaceId, label, new BigDecimal("24.00"), TransactionType.EXPENSE,
            leisure.id(), date, null, List.of(), seriesId, Instant.now());
    }
}
