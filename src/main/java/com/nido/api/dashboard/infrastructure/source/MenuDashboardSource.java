package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.MealItem;
import com.nido.api.dashboard.domain.model.MenuCard;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.kitchen.application.port.in.ListMenuEntriesUseCase;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.Recipe;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Today's and tomorrow's meals, and the days of the coming week with nothing planned. Always present. */
@Component
public class MenuDashboardSource implements DashboardSource {

    static final int WINDOW_DAYS = 7;

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
        LocalDate last = today.plusDays(WINDOW_DAYS - 1);

        Map<LocalDate, List<MealItem>> byDay = listMenuEntries.list(context.caller(), today, last).stream()
            .sorted(Comparator.comparing((MenuEntryView view) -> view.entry().date())
                .thenComparingInt(view -> view.entry().position()))
            .collect(Collectors.groupingBy(view -> view.entry().date(), TreeMap::new,
                Collectors.mapping(MenuDashboardSource::toMeal, Collectors.toList())));

        List<LocalDate> unplanned = today.datesUntil(last.plusDays(1))
            .filter(day -> !byDay.containsKey(day))
            .toList();

        return SourceResult.of(new MenuCard(
            List.copyOf(byDay.getOrDefault(today, List.of())),
            List.copyOf(byDay.getOrDefault(today.plusDays(1), List.of())),
            unplanned));
    }

    private static MealItem toMeal(MenuEntryView view) {
        MenuEntry entry = view.entry();
        Recipe recipe = view.recipe();
        if (recipe == null) {
            // The recipe was deleted after being planned: the slot is still taken.
            return new MealItem(entry.id(), null, null, null, null, entry.portions());
        }
        return new MealItem(entry.id(), recipe.id(), recipe.name(), recipe.category().name(), recipe.minutes(),
            entry.portions());
    }
}
