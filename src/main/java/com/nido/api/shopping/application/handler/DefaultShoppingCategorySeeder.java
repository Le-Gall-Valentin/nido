package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;

import java.util.List;
import java.util.UUID;

/**
 * Seeds a space's default shopping categories the first time anything
 * touches its categories — whether that's listing them (ListShoppingCategoriesHandler)
 * or creating a custom one directly (CreateShoppingCategoryHandler). Sharing this
 * between both entry points guarantees a category can never exist without a
 * fallback category alongside it, regardless of which endpoint a client hits first —
 * a client that only ever calls POST /categories, never GET, previously ended up
 * with no fallback category to reassign a deleted category's items to.
 */
final class DefaultShoppingCategorySeeder {

    static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
        "Fruits & légumes", "Viande & poisson", "Crémerie", "Épicerie", "Surgelés", "Boissons", "Hygiène & entretien");
    static final String DEFAULT_FALLBACK_CATEGORY_NAME = "Maison & divers";

    private DefaultShoppingCategorySeeder() {}

    static void seedIfMissing(ShoppingCategoryRepository categoryRepository, UUID spaceId) {
        if (!categoryRepository.existsBySpaceId(spaceId)) {
            DEFAULT_CATEGORY_NAMES.forEach(name -> categoryRepository.create(spaceId, name, false));
            categoryRepository.create(spaceId, DEFAULT_FALLBACK_CATEGORY_NAME, true);
        }
    }
}
