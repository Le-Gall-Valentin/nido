package com.nido.api.dashboard.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Today on the household's calendar, decided from the events of today and tomorrow and from the tasks
 * due today in board order: all-day banners, the timeline, today's to-dos for everyone — the caller's
 * first — and a glance at tomorrow. Always a card: it is the anchor of the page, even on an empty day.
 */
public final class AgendaDay {

    private static final Comparator<AgendaEvent> ALL_DAY_ORDER =
        Comparator.comparing(AgendaEvent::startDate).thenComparing(AgendaEvent::title);
    private static final Comparator<AgendaEvent> TIMED_ORDER =
        Comparator.comparing(AgendaEvent::startTime, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(AgendaEvent::title);
    /** All-day first, then the earliest start, then the title. */
    private static final Comparator<AgendaEvent> TOMORROW_ORDER =
        Comparator.comparing((AgendaEvent event) -> !event.allDay())
            .thenComparing(event -> event.allDay() ? LocalTime.MIN : event.startTime())
            .thenComparing(AgendaEvent::title);

    private AgendaDay() {}

    public static AgendaCard of(List<AgendaEvent> events, List<TaskItem> dueTodayInBoardOrder, DashboardContext context) {
        LocalDate today = context.today();
        LocalDate tomorrow = today.plusDays(1);

        List<AgendaEvent> allDay = new ArrayList<>();
        List<AgendaEvent> timed = new ArrayList<>();
        for (AgendaEvent event : events) {
            if (event.startDate().isAfter(today) || event.endDate().isBefore(today)) {
                continue;
            }
            // A timed event that starts today has its slot on today's timeline, however late it ends: an
            // evening past midnight still starts at 22:00. One that began on an earlier day has no start
            // today, so it is a banner, like an all-day event.
            if (!event.allDay() && event.startDate().equals(today)) {
                timed.add(event);
            } else {
                allDay.add(event);
            }
        }
        allDay.sort(ALL_DAY_ORDER);
        timed.sort(TIMED_ORDER);

        AgendaEvent first = events.stream()
            .filter(event -> event.startDate().equals(tomorrow))
            .min(TOMORROW_ORDER)
            .orElse(null);

        List<TaskItem> open = dueTodayInBoardOrder.stream()
            .filter(task -> task.status() != TaskItem.Status.DONE)
            .toList();
        List<TaskItem> dueToday = Stream.concat(
                open.stream().filter(TaskItem::mine),
                open.stream().filter(task -> !task.mine()))
            .toList();

        return new AgendaCard(allDay, timed, dueToday, first);
    }
}
