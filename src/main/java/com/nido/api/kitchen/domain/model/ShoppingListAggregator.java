package com.nido.api.kitchen.domain.model;

import com.nido.api.shared.model.MeasurementUnit;
import com.nido.api.shared.model.UnitFamily;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Merges scaled recipe ingredients from a week's menu entries into a
 * shopping list: the same ingredient in compatible units sums into one
 * line, everything else stays separate. Pure and stateless — the caller
 * supplies ingredients already scaled to their entry's portions, flattened
 * in chronological (date, position) order.
 */
public final class ShoppingListAggregator {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final BigDecimal UPGRADE_THRESHOLD = BigDecimal.valueOf(1000);

    public List<ShoppingListLine> aggregate(List<RecipeIngredient> scaledIngredientsInChronologicalOrder) {
        Map<String, Accumulator> groups = new LinkedHashMap<>();
        for (RecipeIngredient ingredient : scaledIngredientsInChronologicalOrder) {
            String mergeKey = normalize(ingredient.name());
            boolean isCount = ingredient.unit().family() == UnitFamily.COUNT;
            String groupKey = mergeKey + "|" + (isCount ? ingredient.unit().name() : ingredient.unit().family().name());
            groups.computeIfAbsent(groupKey, k -> new Accumulator(ingredient.name(), ingredient.unit()))
                .add(ingredient.quantity(), ingredient.unit());
        }
        return groups.values().stream().map(Accumulator::toLine).toList();
    }

    static String normalize(String name) {
        String lowerTrimmed = Normalizer.normalize(name.trim().toLowerCase(), Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(lowerTrimmed).replaceAll("");
        String collapsed = WHITESPACE.matcher(withoutDiacritics).replaceAll(" ");
        return collapsed.length() > 3 && collapsed.endsWith("s")
            ? collapsed.substring(0, collapsed.length() - 1)
            : collapsed;
    }

    private static final class Accumulator {
        private final String displayName;
        private final MeasurementUnit representativeUnit;
        private BigDecimal total = BigDecimal.ZERO;

        Accumulator(String displayName, MeasurementUnit representativeUnit) {
            this.displayName = displayName;
            this.representativeUnit = representativeUnit;
        }

        void add(BigDecimal quantity, MeasurementUnit unit) {
            boolean isCount = unit.family() == UnitFamily.COUNT;
            total = total.add(isCount ? quantity : unit.toBaseUnits(quantity));
        }

        ShoppingListLine toLine() {
            if (representativeUnit.family() == UnitFamily.COUNT) {
                return new ShoppingListLine(displayName, total, representativeUnit);
            }
            boolean mass = representativeUnit.family() == UnitFamily.MASS;
            MeasurementUnit displayUnit = total.compareTo(UPGRADE_THRESHOLD) >= 0
                ? (mass ? MeasurementUnit.KILOGRAM : MeasurementUnit.LITER)
                : (mass ? MeasurementUnit.GRAM : MeasurementUnit.MILLILITER);
            BigDecimal quantity = total.divide(displayUnit.factorToBaseUnit(), 3, RoundingMode.HALF_UP);
            return new ShoppingListLine(displayName, quantity, displayUnit);
        }
    }
}
