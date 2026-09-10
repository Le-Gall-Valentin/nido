package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A recurring series' scheduling fields alone — no title, no priority, no subtask templates,
 * no rotation.
 *
 * <p>Exists so a read path can answer "does this space owe any occurrence?" without loading
 * the full series and its two child collections. Beyond the saved work, the shape is what
 * makes the check safe to run <em>before</em> the materialization lock: a managed entity read
 * beforehand would be handed back unchanged by the read after the lock, since Hibernate keeps
 * the instance it already has and discards the freshly-read column values — a concurrent
 * materialization that committed in between would be invisible, and its occurrences inserted
 * a second time. A projection is not managed, so the read under the lock is a genuine first
 * load. Tasks have no unique constraint to fall back on, so the lock is the only guard.
 */
public record RecurringTaskSeriesSchedule(
    UUID id,
    RecurrenceInterval intervalType,
    int intervalCount,
    RecurrenceInterval leadIntervalType,
    int leadIntervalCount,
    LocalDate anchorDate,
    LocalDate endDate,
    int occurrenceCount
) {}
