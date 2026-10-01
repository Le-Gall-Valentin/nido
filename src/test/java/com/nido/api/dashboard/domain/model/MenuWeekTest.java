package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MenuWeekTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private final DashboardContext context = new DashboardContext(
        new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SpaceRole.MEMBER, Instant.now()),
        TODAY, SpaceType.SHARED);

    @Test
    void theWeekRunsSevenDaysFromToday() {
        assertThat(MenuWeek.lastDay(TODAY)).isEqualTo(TODAY.plusDays(6));
    }

    @Test
    void todayAndTomorrowComeInTheirPositionOrder() {
        MenuCard card = MenuWeek.of(List.of(
            planned(TODAY, 1, "Tarte aux pommes"),
            planned(TODAY.plusDays(1), 0, "Soupe de potiron"),
            planned(TODAY, 0, "Curry de lentilles")), context);

        assertThat(card.today()).extracting(MealItem::recipeName).containsExactly("Curry de lentilles", "Tarte aux pommes");
        assertThat(card.tomorrow()).extracting(MealItem::recipeName).containsExactly("Soupe de potiron");
    }

    @Test
    void unplannedDaysAreTheDatesOfTheWeekWithoutAnyMeal() {
        MenuCard card = MenuWeek.of(List.of(planned(TODAY, 0, "Curry"), planned(TODAY.plusDays(2), 0, "Gratin")), context);

        assertThat(card.unplannedDays()).containsExactly(
            TODAY.plusDays(1), TODAY.plusDays(3), TODAY.plusDays(4), TODAY.plusDays(5), TODAY.plusDays(6));
    }

    @Test
    void anEmptyWeekStillGivesAFullCardWithEveryDayToPlan() {
        MenuCard card = MenuWeek.of(List.of(), context);

        assertThat(card.today()).isEmpty();
        assertThat(card.tomorrow()).isEmpty();
        assertThat(card.unplannedDays()).hasSize(MenuWeek.DAYS).first().isEqualTo(TODAY);
    }

    private static MenuWeek.PlannedMeal planned(LocalDate date, int position, String recipeName) {
        return new MenuWeek.PlannedMeal(date, position,
            new MealItem(UUID.randomUUID(), UUID.randomUUID(), recipeName, MealItem.Category.PLAT, 30, 4));
    }
}
