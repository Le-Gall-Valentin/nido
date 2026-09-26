package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ListOpenTasksUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskOrdering;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListOpenTasksHandler implements ListOpenTasksUseCase {

    private final TaskRepository taskRepository;

    public ListOpenTasksHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> list(SpaceMembership caller) {
        return TaskOrdering.sort(taskRepository.findOpenBySpaceId(caller.spaceId()));
    }
}
