package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.ShoppingAisles;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.shopping.application.port.in.ListShoppingCategoriesUseCase;
import com.nido.api.shopping.application.port.in.ListShoppingItemsUseCase;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingItem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reads the shopping list and, only if something is left to buy, its categories — listing them seeds
 * the defaults under a lock — and lets {@link ShoppingAisles} decide the card.
 */
@Component
public class ShoppingDashboardSource implements DashboardSource {

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
        List<ShoppingAisles.Item> items = listItems.list(context.caller()).stream()
            .map(ShoppingDashboardSource::toItem)
            .toList();
        return ShoppingAisles.of(items, () -> listCategories.list(context.caller()).stream()
            .map(ShoppingDashboardSource::toAisle)
            .toList());
    }

    private static ShoppingAisles.Item toItem(ShoppingItem item) {
        return new ShoppingAisles.Item(item.name(), item.categoryId(), item.position(), item.done());
    }

    private static ShoppingAisles.Aisle toAisle(ShoppingCategory category) {
        return new ShoppingAisles.Aisle(category.id(), category.name(), category.position(), category.fallback());
    }
}
