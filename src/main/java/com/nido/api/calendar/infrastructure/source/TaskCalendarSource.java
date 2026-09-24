package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ListTasksDueBetweenUseCase;
import com.nido.api.tasks.application.port.in.ProjectRecurringTasksUseCase;
import com.nido.api.tasks.domain.model.ProjectedTaskOccurrence;
import com.nido.api.tasks.domain.model.Task;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Tasks that carry a due date, plus the future occurrences of recurring series.
 *
 * <p>A task has no time of day, so every entry is all-day and lands in the day's banner rather
 * than at an invented hour. Tasks without a due date are skipped: they belong on the board, not
 * on a calendar.
 *
 * <p>Only reads. The due occurrences of a recurring series are created by the tasks page; until then
 * the projection shows them, once. Creating them from here, inside the calendar's read-only
 * transaction, failed the whole calendar with a 500.
 */
@Component
public class TaskCalendarSource implements CalendarSource {

    private final ListTasksDueBetweenUseCase listTasksUseCase;
    private final ProjectRecurringTasksUseCase projectRecurringTasksUseCase;

    public TaskCalendarSource(ListTasksDueBetweenUseCase listTasksUseCase,
                              ProjectRecurringTasksUseCase projectRecurringTasksUseCase) {
        this.listTasksUseCase = listTasksUseCase;
        this.projectRecurringTasksUseCase = projectRecurringTasksUseCase;
    }

    @Override
    public CalendarSourceType type() {
        return CalendarSourceType.TASK;
    }

    @Override
    public List<CalendarOccurrence> occurrencesBetween(SpaceMembership caller, LocalDate from, LocalDate to) {
        List<CalendarOccurrence> produced = new ArrayList<>();
        for (Task task : listTasksUseCase.list(caller, from, to)) {
            LocalDate dueDate = task.dueDate();
            produced.add(new CalendarOccurrence(
                CalendarSourceType.TASK, task.id().toString(), task.recurringSeriesId(), null, true,
                task.title(), null, null, true, dueDate, null, dueDate, null, null, task.assigneeIds()));
        }
        for (ProjectedTaskOccurrence projected : projectRecurringTasksUseCase.project(caller, from, to)) {
            produced.add(new CalendarOccurrence(
                CalendarSourceType.TASK,
                CalendarOccurrence.projectedId(projected.seriesId(), projected.dueDate()),
                projected.seriesId(), projected.dueDate(), false,
                projected.title(), null, null, true,
                projected.dueDate(), null, projected.dueDate(), null, null, List.of()));
        }
        return produced;
    }
}
