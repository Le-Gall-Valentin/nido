package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.FinanceStats;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFinanceStatsHandlerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock RecurringTransactionSeriesRepository seriesRepository;
    @Mock BudgetRepository budgetRepository;
    private GetFinanceStatsHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID foodCategory = UUID.randomUUID();
    private final UUID transportCategory = UUID.randomUUID();
    private final UUID incomeCategory = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetFinanceStatsHandler(transactionRepository, seriesRepository, budgetRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    private Transaction transaction(BigDecimal amount, TransactionType type, UUID categoryId) {
        return new Transaction(UUID.randomUUID(), spaceId, "T", amount, type, categoryId,
            LocalDate.of(2026, 1, 15), null, List.of(), null, Instant.now());
    }

    @Test
    void materializes_due_occurrences_before_computing_stats() {
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of());
        when(transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1))).thenReturn(List.of());
        when(budgetRepository.findBySpaceId(spaceId)).thenReturn(List.of());

        handler.getStats(YearMonth.of(2026, 1), membership(), LocalDate.of(2026, 1, 20));

        verify(seriesRepository).findBySpaceId(spaceId);
        verify(seriesRepository).lockForMaterialization(spaceId);
    }

    @Test
    void computes_balance_category_breakdown_and_budget_vs_actual() {
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of());
        when(transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1))).thenReturn(List.of(
            transaction(new BigDecimal("50.00"), TransactionType.EXPENSE, foodCategory),
            transaction(new BigDecimal("30.00"), TransactionType.EXPENSE, transportCategory),
            transaction(new BigDecimal("2000.00"), TransactionType.INCOME, incomeCategory)));
        when(budgetRepository.findBySpaceId(spaceId)).thenReturn(List.of(
            new Budget(UUID.randomUUID(), spaceId, foodCategory, new BigDecimal("400.00"))));

        FinanceStats stats = handler.getStats(YearMonth.of(2026, 1), membership(), LocalDate.of(2026, 1, 20));

        assertThat(stats.totalExpense()).isEqualByComparingTo("80.00");
        assertThat(stats.totalIncome()).isEqualByComparingTo("2000.00");
        assertThat(stats.balance()).isEqualByComparingTo("1920.00");
        // Every category appears here, not just expense ones — the frontend splits this
        // list by the category's own type for its two breakdown views.
        assertThat(stats.breakdown()).containsExactlyInAnyOrder(
            new com.nido.api.finance.domain.model.CategoryAmount(foodCategory, new BigDecimal("50.00")),
            new com.nido.api.finance.domain.model.CategoryAmount(transportCategory, new BigDecimal("30.00")),
            new com.nido.api.finance.domain.model.CategoryAmount(incomeCategory, new BigDecimal("2000.00")));
        assertThat(stats.budgetVsActual()).containsExactly(
            new com.nido.api.finance.domain.model.BudgetLine(foodCategory, new BigDecimal("400.00"), new BigDecimal("50.00")));
        // Only foodCategory is budgeted (400.00 limit, 50.00 spent so far): the 30.00 spent in
        // transportCategory has no budget at all, so it must not eat into this figure — a
        // category left deliberately unbudgeted has no cap, and therefore no effect here either.
        assertThat(stats.remainingBudget()).isEqualByComparingTo("350.00");
    }

    @Test
    void remaining_budget_ignores_spending_in_categories_that_have_no_budget_at_all() {
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of());
        when(transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1))).thenReturn(List.of(
            transaction(new BigDecimal("500.00"), TransactionType.EXPENSE, transportCategory)));
        when(budgetRepository.findBySpaceId(spaceId)).thenReturn(List.of(
            new Budget(UUID.randomUUID(), spaceId, foodCategory, new BigDecimal("400.00"))));

        FinanceStats stats = handler.getStats(YearMonth.of(2026, 1), membership(), LocalDate.of(2026, 1, 20));

        assertThat(stats.remainingBudget()).isEqualByComparingTo("400.00");
    }
}
