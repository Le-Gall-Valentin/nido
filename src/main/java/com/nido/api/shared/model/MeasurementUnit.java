package com.nido.api.shared.model;

import java.math.BigDecimal;

public enum MeasurementUnit {
    GRAM(UnitFamily.MASS, BigDecimal.ONE),
    KILOGRAM(UnitFamily.MASS, BigDecimal.valueOf(1000)),
    MILLILITER(UnitFamily.VOLUME, BigDecimal.ONE),
    CENTILITER(UnitFamily.VOLUME, BigDecimal.TEN),
    LITER(UnitFamily.VOLUME, BigDecimal.valueOf(1000)),
    PIECE(UnitFamily.COUNT, BigDecimal.ONE),
    SLICE(UnitFamily.COUNT, BigDecimal.ONE),
    TABLESPOON(UnitFamily.COUNT, BigDecimal.ONE),
    TEASPOON(UnitFamily.COUNT, BigDecimal.ONE),
    PINCH(UnitFamily.COUNT, BigDecimal.ONE),
    SACHET(UnitFamily.COUNT, BigDecimal.ONE);

    private final UnitFamily family;
    private final BigDecimal factorToBaseUnit;

    MeasurementUnit(UnitFamily family, BigDecimal factorToBaseUnit) {
        this.family = family;
        this.factorToBaseUnit = factorToBaseUnit;
    }

    public UnitFamily family() {
        return family;
    }

    public BigDecimal factorToBaseUnit() {
        return factorToBaseUnit;
    }

    /**
     * Converts a quantity in this unit to the family's base unit (gram for MASS,
     * milliliter for VOLUME). Meaningless across families — callers only ever use
     * this to compare/sum quantities already known to share a family.
     */
    public BigDecimal toBaseUnits(BigDecimal quantity) {
        return quantity.multiply(factorToBaseUnit);
    }
}
