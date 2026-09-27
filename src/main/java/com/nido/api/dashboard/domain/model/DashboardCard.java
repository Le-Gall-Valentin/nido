package com.nido.api.dashboard.domain.model;

/** The six cards a dashboard can show. */
public sealed interface DashboardCard
    permits AgendaCard, MenuCard, TasksCard, FinanceCard, SavingsCard, ShoppingCard {
}
