package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** {@code balances} is null in a personal space. */
public record FinanceCard(YearMonth month, BigDecimal balance, BigDecimal totalExpense, BigDecimal totalIncome,
                          BigDecimal remainingBudget, List<BudgetWatch> budgetsToWatch,
                          List<UpcomingOperation> upcoming, List<BalanceWithMember> balances) implements DashboardCard {
}
