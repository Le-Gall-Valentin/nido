package com.nido.api.kitchen.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementUnitTest {

    @Test
    void kilogram_converts_to_1000_grams() {
        assertThat(MeasurementUnit.KILOGRAM.toBaseUnits(BigDecimal.valueOf(2)))
            .isEqualByComparingTo("2000");
        assertThat(MeasurementUnit.KILOGRAM.family()).isEqualTo(UnitFamily.MASS);
    }

    @Test
    void centiliter_converts_to_10_milliliters() {
        assertThat(MeasurementUnit.CENTILITER.toBaseUnits(BigDecimal.valueOf(3)))
            .isEqualByComparingTo("30");
        assertThat(MeasurementUnit.CENTILITER.family()).isEqualTo(UnitFamily.VOLUME);
    }

    @Test
    void gram_and_milliliter_are_their_own_base_unit() {
        assertThat(MeasurementUnit.GRAM.toBaseUnits(BigDecimal.valueOf(42)))
            .isEqualByComparingTo("42");
        assertThat(MeasurementUnit.MILLILITER.toBaseUnits(BigDecimal.valueOf(42)))
            .isEqualByComparingTo("42");
    }

    @Test
    void piece_belongs_to_the_count_family() {
        assertThat(MeasurementUnit.PIECE.family()).isEqualTo(UnitFamily.COUNT);
        assertThat(MeasurementUnit.SLICE.family()).isEqualTo(UnitFamily.COUNT);
    }
}
