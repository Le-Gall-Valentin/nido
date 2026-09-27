package com.nido.api.dashboard.domain.model;

import java.util.List;

public record SavingsCard(List<SavingsGoalItem> goals) implements DashboardCard {

    public SavingsCard {
        goals = List.copyOf(goals);
    }
}
