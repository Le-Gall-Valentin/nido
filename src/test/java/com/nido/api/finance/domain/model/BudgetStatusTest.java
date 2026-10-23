package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetStatusTest {

    @Test
    void each_status_is_worse_than_the_ones_below_it() {
        assertThat(BudgetStatus.OVER.isWorseThan(BudgetStatus.WARNING)).isTrue();
        assertThat(BudgetStatus.OVER.isWorseThan(BudgetStatus.OK)).isTrue();
        assertThat(BudgetStatus.WARNING.isWorseThan(BudgetStatus.OK)).isTrue();
    }

    @Test
    void no_status_is_worse_than_itself_or_than_a_worse_one() {
        assertThat(BudgetStatus.OK.isWorseThan(BudgetStatus.OK)).isFalse();
        assertThat(BudgetStatus.WARNING.isWorseThan(BudgetStatus.OVER)).isFalse();
        assertThat(BudgetStatus.OK.isWorseThan(BudgetStatus.WARNING)).isFalse();
    }
}
