package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.FinanceStats;

import java.math.BigDecimal;
import java.util.List;

public record FinanceStatsResponse(
    BigDecimal balance, BigDecimal totalExpense, BigDecimal totalIncome, BigDecimal remainingBudget,
    List<CategoryAmountResponse> breakdown, List<BudgetLineResponse> budgetVsActual
) {
    public static FinanceStatsResponse from(FinanceStats s) {
        return new FinanceStatsResponse(s.balance(), s.totalExpense(), s.totalIncome(), s.remainingBudget(),
            s.breakdown().stream().map(CategoryAmountResponse::from).toList(),
            s.budgetVsActual().stream().map(BudgetLineResponse::from).toList());
    }
}
