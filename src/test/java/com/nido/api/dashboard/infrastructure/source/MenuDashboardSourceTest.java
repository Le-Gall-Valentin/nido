package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.MealItem;
import com.nido.api.dashboard.domain.model.MenuCard;
import com.nido.api.kitchen.application.port.in.ListMenuEntriesUseCase;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuDashboardSourceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    private final ListMenuEntriesUseCase listMenuEntries = mock(ListMenuEntriesUseCase.class);

    @Test
    void todayAndTomorrowComeInTheirPositionOrder() {
        when(listMenuEntries.list(caller, TODAY, TODAY.plusDays(6))).thenReturn(List.of(
            entry(TODAY, 1, recipe("Tarte aux pommes", RecipeCategory.DESSERT, 50)),
            entry(TODAY.plusDays(1), 0, recipe("Soupe de potiron", RecipeCategory.SOUP, 40)),
            entry(TODAY, 0, recipe("Curry de lentilles", RecipeCategory.VEGETARIAN, 35))));

        MenuCard card = read();

        assertThat(card.today()).extracting(MealItem::recipeName).containsExactly("Curry de lentilles", "Tarte aux pommes");
        assertThat(card.tomorrow()).extracting(MealItem::recipeName).containsExactly("Soupe de potiron");
    }

    @Test
    void aMealCarriesItsRecipeAndItsOwnPortions() {
        Recipe curry = recipe("Curry de lentilles", RecipeCategory.VEGETARIAN, 35);
        MenuEntryView view = entry(TODAY, 0, curry);
        when(listMenuEntries.list(caller, TODAY, TODAY.plusDays(6))).thenReturn(List.of(view));

        assertThat(read().today()).containsExactly(
            new MealItem(view.entry().id(), curry.id(), "Curry de lentilles", "VEGETARIAN", 35, 6));
    }

    @Test
    void aDeletedRecipeStillShowsItsEntryWithoutAName() {
        MenuEntryView orphan = entry(TODAY, 0, null);
        when(listMenuEntries.list(caller, TODAY, TODAY.plusDays(6))).thenReturn(List.of(orphan));

        assertThat(read().today()).containsExactly(new MealItem(orphan.entry().id(), null, null, null, null, 6));
    }

    @Test
    void unplannedDaysAreTheDatesOfTheSevenDaysWithoutAnyEntry() {
        when(listMenuEntries.list(caller, TODAY, TODAY.plusDays(6))).thenReturn(List.of(
            entry(TODAY, 0, recipe("Curry", RecipeCategory.PLAT, 30)),
            entry(TODAY.plusDays(2), 0, recipe("Gratin", RecipeCategory.PLAT, 60))));

        assertThat(read().unplannedDays()).containsExactly(
            TODAY.plusDays(1), TODAY.plusDays(3), TODAY.plusDays(4), TODAY.plusDays(5), TODAY.plusDays(6));
    }

    @Test
    void anEmptyWeekStillGivesAFullCardWithSevenUnplannedDays() {
        when(listMenuEntries.list(caller, TODAY, TODAY.plusDays(6))).thenReturn(List.of());

        MenuCard card = read();

        assertThat(card.today()).isEmpty();
        assertThat(card.tomorrow()).isEmpty();
        assertThat(card.unplannedDays()).hasSize(7).first().isEqualTo(TODAY);
    }

    @Test
    void itIsTheMenuSource() {
        assertThat(new MenuDashboardSource(listMenuEntries).kind()).isEqualTo(CardKind.MENU);
    }

    private MenuCard read() {
        return (MenuCard) new MenuDashboardSource(listMenuEntries)
            .read(new DashboardContext(caller, "me@test.com", TODAY, SpaceType.SHARED)).card();
    }

    private MenuEntryView entry(LocalDate date, int position, Recipe recipe) {
        UUID recipeId = recipe == null ? UUID.randomUUID() : recipe.id();
        return new MenuEntryView(new MenuEntry(UUID.randomUUID(), spaceId, date, recipeId, 6, position), recipe);
    }

    private Recipe recipe(String name, RecipeCategory category, int minutes) {
        return new Recipe(UUID.randomUUID(), spaceId, name, null, category, minutes, 4, false,
            List.of(), List.of(), null, Instant.now(), Instant.now());
    }
}
