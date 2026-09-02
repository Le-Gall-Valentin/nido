package com.nido.api.shopping.domain.model;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Merge key for matching a shopping item against an import line by name,
 * independent of case, accents, whitespace, or a naive French plural.
 * Deliberately duplicated from kitchen.ShoppingListAggregator.normalize
 * rather than shared — it's a one-line string utility, not worth a
 * cross-BC dependency for.
 */
public final class ShoppingItemNameNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    private ShoppingItemNameNormalizer() {}

    public static String normalize(String name) {
        String lowerTrimmed = Normalizer.normalize(name.trim().toLowerCase(), Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(lowerTrimmed).replaceAll("");
        String collapsed = WHITESPACE.matcher(withoutDiacritics).replaceAll(" ");
        return collapsed.length() > 3 && collapsed.endsWith("s")
            ? collapsed.substring(0, collapsed.length() - 1)
            : collapsed;
    }
}
