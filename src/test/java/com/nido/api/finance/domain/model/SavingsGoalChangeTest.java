package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A goal changes for its members when its name, target or date does — not its colour or its icon. */
class SavingsGoalChangeTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SPACE = UUID.randomUUID();

    private static SavingsGoal goal(String name, String target, LocalDate date, String color, String glyph) {
        return new SavingsGoal(ID, SPACE, name, new BigDecimal(target), date, color, glyph);
    }

    private static SavingsGoal holidays() {
        return goal("Vacances", "1000.00", LocalDate.of(2027, 7, 1), "#c17a5c", "🏖️");
    }

    @Test
    void the_name_the_target_and_the_date_are_changes() {
        assertThat(goal("Voyage", "1000.00", LocalDate.of(2027, 7, 1), "#c17a5c", "🏖️").differsForMembersFrom(holidays())).isTrue();
        assertThat(goal("Vacances", "1200.00", LocalDate.of(2027, 7, 1), "#c17a5c", "🏖️").differsForMembersFrom(holidays())).isTrue();
        assertThat(goal("Vacances", "1000.00", null, "#c17a5c", "🏖️").differsForMembersFrom(holidays())).isTrue();
    }

    @Test
    void a_colour_or_an_icon_alone_is_no_change() {
        assertThat(goal("Vacances", "1000.00", LocalDate.of(2027, 7, 1), "#5c7a58", "✈️").differsForMembersFrom(holidays())).isFalse();
    }

    @Test
    void a_target_of_another_scale_is_no_change() {
        assertThat(goal("Vacances", "1000.0", LocalDate.of(2027, 7, 1), "#c17a5c", "🏖️").differsForMembersFrom(holidays())).isFalse();
    }
}
