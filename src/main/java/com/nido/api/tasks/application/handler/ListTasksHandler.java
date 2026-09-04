package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ListTasksUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskOrdering;
import com.nido.api.tasks.domain.port.out.TaskRepository;

import java.util.List;

@ApplicationService
public class ListTasksHandler implements ListTasksUseCase {

    private final TaskRepository taskRepository;

    public ListTasksHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public List<Task> list(SpaceMembership caller) {
        return TaskOrdering.sort(taskRepository.findBySpaceId(caller.spaceId()));
    }
}
