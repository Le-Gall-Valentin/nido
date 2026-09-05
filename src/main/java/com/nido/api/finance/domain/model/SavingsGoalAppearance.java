package com.nido.api.finance.domain.model;

import java.util.List;

public final class SavingsGoalAppearance {

    public static final List<String> COLORS =
        List.of("#5c7a58", "#c17a5c", "#4a7fa0", "#7a6f9c", "#c9736a");

    public static final List<String> GLYPHS =
        List.of("🎯", "🏖️", "🏠", "🚗", "🎁", "💍", "🎓", "🛟");

    private SavingsGoalAppearance() {}

    /** L'apparence d'un objectif est limitée à la palette du design : le client ne choisit pas librement. */
    public static void ensureValid(String color, String glyph) {
        ensureValidColor(color);
        ensureValidGlyph(glyph);
    }

    public static void ensureValidColor(String color) {
        if (!COLORS.contains(color)) {
            throw new FinanceException.InvalidSavingsGoalAppearance();
        }
    }

    public static void ensureValidGlyph(String glyph) {
        if (!GLYPHS.contains(glyph)) {
            throw new FinanceException.InvalidSavingsGoalAppearance();
        }
    }
}
