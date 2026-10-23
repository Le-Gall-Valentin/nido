package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/** A category's spending crossing a threshold of its budget, and the operation that took it there. */
public record BudgetAlert(String categoryLabel, YearMonth month, BigDecimal spent, BigDecimal limit, String causeLabel,
                          BigDecimal causeAmount) {

    /** How much of the budget is spent, in whole percent rounded down; a budget of zero is all spent. */
    public int percent() {
        return Percent.of(spent, limit);
    }

    /** How far past the budget the spending went; zero while it is within it. */
    public BigDecimal overrun() {
        return spent.subtract(limit).max(BigDecimal.ZERO);
    }
}
