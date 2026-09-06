package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.CategoryRepository;

import java.util.List;
import java.util.UUID;

/**
 * Seeds a space's default Finance categories the first time anything
 * touches its categories — mirrors {@code DefaultShoppingCategorySeeder}:
 * shared between listing and creating a custom category so a category can
 * never exist without its defaults alongside it, regardless of which
 * endpoint a client hits first.
 */
final class DefaultFinanceCategorySeeder {

    private record DefaultCategory(String label, String color, String icon, TransactionType type) {}

    private static final List<DefaultCategory> DEFAULTS = List.of(
        new DefaultCategory("Alimentation", "#f59e0b", "Utensils", TransactionType.EXPENSE),
        new DefaultCategory("Logement", "#6366f1", "Home", TransactionType.EXPENSE),
        new DefaultCategory("Transport", "#0ea5e9", "Car", TransactionType.EXPENSE),
        new DefaultCategory("Loisirs", "#ec4899", "Gamepad2", TransactionType.EXPENSE),
        new DefaultCategory("Santé", "#ef4444", "HeartPulse", TransactionType.EXPENSE),
        new DefaultCategory("Abonnements", "#8b5cf6", "Repeat", TransactionType.EXPENSE),
        new DefaultCategory("Divers", "#64748b", "MoreHorizontal", TransactionType.EXPENSE),
        new DefaultCategory("Revenu", "#22c55e", "Wallet", TransactionType.INCOME));

    private DefaultFinanceCategorySeeder() {}

    static void seedIfMissing(CategoryRepository categoryRepository, UUID spaceId) {
        categoryRepository.lockForSeeding(spaceId);
        if (!categoryRepository.existsBySpaceId(spaceId)) {
            DEFAULTS.forEach(d -> categoryRepository.create(
                new CreateCategoryCommand(spaceId, d.label(), d.color(), d.icon(), d.type()), true));
        }
    }
}
