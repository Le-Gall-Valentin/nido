package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record FinanceStats(
    BigDecimal balance, BigDecimal totalExpense, BigDecimal totalIncome, BigDecimal remainingBudget,
    List<CategoryAmount> breakdown, List<BudgetLine> budgetVsActual
) {}
