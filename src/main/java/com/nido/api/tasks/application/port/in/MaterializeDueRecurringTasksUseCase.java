package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

/**
 * Creates the recurring task occurrences that have fallen due, exactly as opening the tasks page
 * does — same algorithm, same advisory lock.
 *
 * <p>A write. Call it in a transaction of its own, never from inside a read-only one: that is what
 * failed the whole calendar with a 500 (commit 83fa623).
 */
public interface MaterializeDueRecurringTasksUseCase {
    void materialize(SpaceMembership caller);
}
