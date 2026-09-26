package com.nido.api.dashboard.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code priority} and {@code status} are the tasks module's enum names, carried as text. */
public record TaskItem(UUID id, String title, LocalDate dueDate, String priority, String status,
                       List<UUID> assigneeIds, int subtasksDone, int subtasksTotal, boolean recurring) {
}
