package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.ShoppingCard;
import com.nido.api.dashboard.domain.model.ShoppingGroup;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.shopping.application.port.in.ListShoppingCategoriesUseCase;
import com.nido.api.shopping.application.port.in.ListShoppingItemsUseCase;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** What is left to buy, aisle by aisle. Absent when the list is done. */
@Component
public class ShoppingDashboardSource implements DashboardSource {

    static final int PREVIEW_SIZE = 3;

    private final ListShoppingItemsUseCase listItems;
    private final ListShoppingCategoriesUseCase listCategories;

    public ShoppingDashboardSource(ListShoppingItemsUseCase listItems, ListShoppingCategoriesUseCase listCategories) {
        this.listItems = listItems;
        this.listCategories = listCategories;
    }

    @Override
    public CardKind kind() {
        return CardKind.SHOPPING;
    }

    @Override
    public SourceResult read(DashboardContext context) {
        List<ShoppingItem> undone = listItems.list(context.caller()).stream()
            .filter(item -> !item.done())
            .sorted(Comparator.comparingInt(ShoppingItem::position))
            .toList();
        if (undone.isEmpty()) {
            // Checked before the categories on purpose: listing them seeds the defaults under a lock.
            return SourceResult.nothing();
        }

        List<ShoppingCategory> categories = listCategories.list(context.caller()).stream()
            .sorted(Comparator.comparingInt(ShoppingCategory::position))
            .toList();
        Set<UUID> known = categories.stream().map(ShoppingCategory::id).collect(Collectors.toSet());
        UUID fallbackId = categories.stream().filter(ShoppingCategory::fallback)
            .map(ShoppingCategory::id).findFirst().orElse(null);

        Map<UUID, List<ShoppingItem>> byCategory = new HashMap<>();
        for (ShoppingItem item : undone) {
            UUID categoryId = known.contains(item.categoryId()) ? item.categoryId() : fallbackId;
            if (categoryId != null) {
                byCategory.computeIfAbsent(categoryId, id -> new ArrayList<>()).add(item);
            }
        }

        List<ShoppingGroup> groups = categories.stream()
            .filter(category -> byCategory.containsKey(category.id()))
            .map(category -> {
                List<ShoppingItem> items = byCategory.get(category.id());
                return new ShoppingGroup(category.id(), category.name(), items.size(),
                    items.stream().limit(PREVIEW_SIZE).map(ShoppingItem::name).toList());
            })
            .toList();
        return SourceResult.of(new ShoppingCard(undone.size(), groups));
    }
}
