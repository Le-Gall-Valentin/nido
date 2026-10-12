package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTypeTest {

    @Test
    void an_income_raises_a_balance_and_an_expense_lowers_it() {
        assertThat(TransactionType.INCOME.signed(new BigDecimal("12.50"))).isEqualByComparingTo("12.50");
        assertThat(TransactionType.EXPENSE.signed(new BigDecimal("12.50"))).isEqualByComparingTo("-12.50");
    }
}
