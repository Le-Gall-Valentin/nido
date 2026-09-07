package com.nido.api.tasks.domain.port.out;

import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    Optional<Task> findById(UUID taskId);
    List<Task> findBySpaceId(UUID spaceId);
    Task create(CreateTaskCommand command);
    /**
     * Persists every command as a single batch instead of one round trip per task —
     * used by {@code RecurringTaskSeriesMaterializer} so catching up on many overdue
     * occurrences at once costs one batched write, not one write per occurrence.
     * A no-op for an empty list.
     */
    void createAll(List<CreateTaskCommand> commands);
    Task update(UpdateTaskCommand command);
    Task updateStatus(UUID taskId, TaskStatus status);
    Task toggleSubtask(UUID taskId, UUID subtaskId);
    void delete(UUID taskId);
}
