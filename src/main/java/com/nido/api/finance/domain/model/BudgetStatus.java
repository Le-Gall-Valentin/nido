package com.nido.api.finance.domain.model;

/** Where a month's spending in a category stands against that category's budget, from best to worst. */
public enum BudgetStatus {
    OK, WARNING, OVER;

    public boolean isWorseThan(BudgetStatus other) {
        return compareTo(other) > 0;
    }
}
