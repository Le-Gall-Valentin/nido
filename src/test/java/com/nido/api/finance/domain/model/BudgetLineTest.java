package com.nido.api.finance.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetLineTest {

    @ParameterizedTest(name = "limit {0}, spent {1} -> {2}")
    @CsvSource({
        "400.00, 0.00,   OK",
        "400.00, 319.99, OK",
        // exactly 80 % is already a warning
        "400.00, 320.00, WARNING",
        // exactly the limit is still within it
        "400.00, 400.00, WARNING",
        "400.00, 400.01, OVER",
        // 0 € is a deliberate "spend nothing here" cap, not "no budget"
        "0.00,   0.00,   OK",
        "0.00,   0.01,   OVER",
    })
    void status_follows_the_share_of_the_limit_already_spent(String limit, String spent, BudgetStatus expected) {
        BudgetLine line = new BudgetLine(UUID.randomUUID(), new BigDecimal(limit), new BigDecimal(spent));

        assertThat(line.status()).isEqualTo(expected);
    }
}
