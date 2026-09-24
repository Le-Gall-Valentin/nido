package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.Task;

import java.time.LocalDate;
import java.util.List;

/**
 * The tasks already created that fall due inside a window — for a reader that shows dates, such as
 * the calendar.
 *
 * <p>Unlike {@link ListTasksUseCase}, it creates nothing: the recurring occurrences that have come
 * due are left to the tasks page, and {@link ProjectRecurringTasksUseCase} shows them in the meantime.
 */
public interface ListTasksDueBetweenUseCase {
    List<Task> list(SpaceMembership caller, LocalDate from, LocalDate to);
}
