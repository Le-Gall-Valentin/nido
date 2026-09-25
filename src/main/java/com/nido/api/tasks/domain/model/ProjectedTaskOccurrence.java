package com.nido.api.tasks.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A future occurrence of a recurring task series, computed in memory and never persisted.
 *
 * <p>Tasks materializes occurrences only up to today, so without this the future of a recurring
 * task exists nowhere. Mirrors Finance's {@code ProjectedOccurrence}, which serves the same
 * purpose on that side.
 *
 * <p>Carries no assignee, deliberately: on a rotating series the holder is decided when the
 * occurrence is materialized, so any name attached here would be a guess that a late or early
 * materialization silently invalidates. An empty slot is honest; a wrong name is not.
 */
public record ProjectedTaskOccurrence(UUID seriesId, String title, TaskPriority priority, LocalDate dueDate) {}
