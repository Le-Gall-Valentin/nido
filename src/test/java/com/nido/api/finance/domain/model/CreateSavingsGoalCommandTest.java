package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateSavingsGoalCommandTest {

    private static final UUID SPACE_ID = UUID.randomUUID();

    @Test
    void rejects_a_color_outside_the_palette() {
        assertThatThrownBy(() -> new CreateSavingsGoalCommand(SPACE_ID, "Vacances", new BigDecimal("2000.00"), null, "#123456", "🎯"))
            .isInstanceOf(FinanceException.InvalidSavingsGoalAppearance.class);
    }

    @Test
    void rejects_a_glyph_outside_the_palette() {
        assertThatThrownBy(() -> new CreateSavingsGoalCommand(SPACE_ID, "Vacances", new BigDecimal("2000.00"), null, "#5c7a58", "🐙"))
            .isInstanceOf(FinanceException.InvalidSavingsGoalAppearance.class);
    }
}
