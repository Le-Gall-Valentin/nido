package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code balance} is carried over from every month before: today's for the current month, the month end's for any
 * other — the day is {@code balanceAsOf}. {@code monthNet} is the month's own result, its income minus its expenses.
 */
public record FinanceStats(
    BigDecimal balance, LocalDate balanceAsOf, BigDecimal monthNet, BigDecimal totalExpense, BigDecimal totalIncome,
    BigDecimal remainingBudget, List<CategoryAmount> breakdown, List<BudgetLine> budgetVsActual
) {}
