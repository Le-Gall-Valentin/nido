package com.nido.api.finance.domain.model;

import java.math.BigDecimal;

public enum TransactionType {
    EXPENSE, INCOME;

    /** What an operation of this type moves a balance by: up for an income, down for an expense. */
    public BigDecimal signed(BigDecimal amount) {
        return this == INCOME ? amount : amount.negate();
    }
}
