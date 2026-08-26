package com.nido.api.space.domain.model;

import java.util.List;

public final class SpaceAppearance {

    public static final List<String> ACCENTS =
        List.of("#c17a5c", "#4a7fa0", "#5c7a58", "#7a6f9c", "#c98aa6", "#c9a24e");

    public static final List<String> GLYPHS =
        List.of("🏡", "🏠", "🌷", "🌿", "🐣", "🎒", "🍀", "🚗");

    public static final String PERSONAL_ACCENT = "#8a7d6b";
    public static final String PERSONAL_GLYPH = "👤";

    private SpaceAppearance() {}

    /** L'apparence d'un groupe est limitée à la palette du design : le client ne choisit pas librement. */
    public static void ensureValid(String accent, String glyph) {
        ensureValidAccent(accent);
        ensureValidGlyph(glyph);
    }

    /** Validation d'un seul champ, pour une modification partielle. */
    public static void ensureValidAccent(String accent) {
        if (!ACCENTS.contains(accent)) {
            throw new SpaceException.InvalidAppearance();
        }
    }

    /** Validation d'un seul champ, pour une modification partielle. */
    public static void ensureValidGlyph(String glyph) {
        if (!GLYPHS.contains(glyph)) {
            throw new SpaceException.InvalidAppearance();
        }
    }
}
