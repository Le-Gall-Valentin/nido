package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsGoalDetailTest {

    private final UUID goalId = UUID.randomUUID();
    private final SavingsGoal goal = new SavingsGoal(goalId, UUID.randomUUID(), "Vacances",
        new BigDecimal("3000.00"), LocalDate.of(2027, 6, 30), "#44618a", "🏖️");

    @Test
    void totalContributed_sums_every_contribution() {
        SavingsGoalDetail detail = new SavingsGoalDetail(goal, List.of(
            contribution("1000.00"), contribution("450.50")));

        assertThat(detail.totalContributed()).isEqualByComparingTo("1450.50");
    }

    @Test
    void totalContributed_is_zero_without_any_contribution() {
        assertThat(new SavingsGoalDetail(goal, List.of()).totalContributed()).isEqualByComparingTo("0");
    }

    private SavingsContribution contribution(String amount) {
        return new SavingsContribution(UUID.randomUUID(), goalId, UUID.randomUUID(), new BigDecimal(amount), LocalDate.of(2026, 9, 1));
    }
}
