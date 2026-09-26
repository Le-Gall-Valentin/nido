package com.nido.api.dashboard.domain.model;

import java.util.List;

public record TasksCard(List<TaskItem> overdue, List<TaskItem> thisWeek, List<TaskItem> inProgress,
                        int openCount, int openCountMine) implements DashboardCard {
}
