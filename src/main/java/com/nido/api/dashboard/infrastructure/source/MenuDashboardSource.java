package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.MealItem;
import com.nido.api.dashboard.domain.model.MenuWeek;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.kitchen.application.port.in.ListMenuEntriesUseCase;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Reads the week's menu from the kitchen module and lets {@link MenuWeek} decide the card. */
@Component
public class MenuDashboardSource implements DashboardSource {

    private final ListMenuEntriesUseCase listMenuEntries;

    public MenuDashboardSource(ListMenuEntriesUseCase listMenuEntries) {
        this.listMenuEntries = listMenuEntries;
    }

    @Override
    public CardKind kind() {
        return CardKind.MENU;
    }

    @Override
    public SourceResult read(DashboardContext context) {
        LocalDate today = context.today();
        List<MenuWeek.PlannedMeal> planned = listMenuEntries.list(context.caller(), today, MenuWeek.lastDay(today)).stream()
            .map(MenuDashboardSource::toPlanned)
            .toList();
        return SourceResult.of(MenuWeek.of(planned, context));
    }

    private static MenuWeek.PlannedMeal toPlanned(MenuEntryView view) {
        MenuEntry entry = view.entry();
        return new MenuWeek.PlannedMeal(entry.date(), entry.position(), toMeal(view));
    }

    private static MealItem toMeal(MenuEntryView view) {
        MenuEntry entry = view.entry();
        Recipe recipe = view.recipe();
        if (recipe == null) {
            // The recipe was deleted after being planned: the slot is still taken.
            return new MealItem(entry.id(), null, null, null, null, entry.portions());
        }
        return new MealItem(entry.id(), recipe.id(), recipe.name(), categoryOf(recipe.category()), recipe.minutes(),
            entry.portions());
    }

    static MealItem.Category categoryOf(RecipeCategory category) {
        return switch (category) {
            case PLAT -> MealItem.Category.PLAT;
            case EXPRESS -> MealItem.Category.EXPRESS;
            case VEGETARIAN -> MealItem.Category.VEGETARIAN;
            case DESSERT -> MealItem.Category.DESSERT;
            case SOUP -> MealItem.Category.SOUP;
        };
    }
}
