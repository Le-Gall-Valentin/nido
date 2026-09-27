package com.nido.api.dashboard.domain.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** The menu card: today's and tomorrow's meals, and the days of the coming week with nothing planned. Always a card. */
public final class MenuWeek {

    /** The week the card looks at, today included. */
    public static final int DAYS = 7;

    /** A meal planned on a day, at its place in that day's menu. */
    public record PlannedMeal(LocalDate date, int position, MealItem meal) {
    }

    private MenuWeek() {}

    /** The last day of the week that starts today. */
    public static LocalDate lastDay(LocalDate today) {
        return today.plusDays(DAYS - 1);
    }

    public static MenuCard of(List<PlannedMeal> planned, DashboardContext context) {
        LocalDate today = context.today();
        Map<LocalDate, List<MealItem>> byDay = planned.stream()
            .sorted(Comparator.comparing(PlannedMeal::date).thenComparingInt(PlannedMeal::position))
            .collect(Collectors.groupingBy(PlannedMeal::date, TreeMap::new,
                Collectors.mapping(PlannedMeal::meal, Collectors.toList())));

        List<LocalDate> unplanned = today.datesUntil(lastDay(today).plusDays(1))
            .filter(day -> !byDay.containsKey(day))
            .toList();

        return new MenuCard(byDay.getOrDefault(today, List.of()), byDay.getOrDefault(today.plusDays(1), List.of()),
            unplanned);
    }
}
