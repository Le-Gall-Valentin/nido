package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.Task;

import java.util.List;

/**
 * Every task of the space that is not done — overdue, due later or never — in board order.
 *
 * <p>A pure read: unlike {@link ListTasksUseCase} it creates no recurring occurrence, so it is safe
 * inside a read-only transaction. A reader that wants the occurrences that have fallen due calls
 * {@code MaterializeDueRecurringTasksUseCase} first, in a transaction of its own.
 */
public interface ListOpenTasksUseCase {
    List<Task> list(SpaceMembership caller);
}
