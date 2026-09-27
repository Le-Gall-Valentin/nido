package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetLine(UUID categoryId, BigDecimal monthlyLimit, BigDecimal spent) {

    private static final BigDecimal WARNING_NUMERATOR = BigDecimal.valueOf(8);
    private static final BigDecimal WARNING_DENOMINATOR = BigDecimal.TEN;

    /**
     * {@code OVER} past the limit, {@code WARNING} from 80 % of it up to the limit itself, {@code OK}
     * below. A limit of exactly zero is a deliberate "spend nothing here" cap rather than "no budget"
     * (a category without a budget never gets a line at all), so the first cent spent is an overrun.
     *
     * <p>The 80 % test is a cross-multiplication — {@code spent × 10 ≥ limit × 8} — so no division
     * ever rounds a ratio across the threshold.
     */
    public BudgetStatus status() {
        if (monthlyLimit.signum() == 0) {
            return spent.signum() > 0 ? BudgetStatus.OVER : BudgetStatus.OK;
        }
        if (spent.compareTo(monthlyLimit) > 0) {
            return BudgetStatus.OVER;
        }
        if (spent.multiply(WARNING_DENOMINATOR).compareTo(monthlyLimit.multiply(WARNING_NUMERATOR)) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.OK;
    }
}
