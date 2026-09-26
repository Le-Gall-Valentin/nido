package com.nido.api.dashboard.domain.model;

import java.time.LocalDate;
import java.util.List;

public record MenuCard(List<MealItem> today, List<MealItem> tomorrow, List<LocalDate> unplannedDays)
    implements DashboardCard {
}
