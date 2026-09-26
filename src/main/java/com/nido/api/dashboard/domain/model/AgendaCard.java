package com.nido.api.dashboard.domain.model;

import java.util.List;

/** {@code tomorrow} is the first event starting tomorrow, or null. */
public record AgendaCard(List<AgendaEvent> allDay, List<AgendaEvent> timed, List<TaskItem> dueToday,
                         AgendaEvent tomorrow) implements DashboardCard {
}
