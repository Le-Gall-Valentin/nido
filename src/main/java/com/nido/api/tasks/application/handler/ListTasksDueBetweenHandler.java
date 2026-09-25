package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.ListTasksDueBetweenUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * A pure read, safe inside any read-only transaction. It deliberately does not materialize: doing so
 * from the calendar's read-only transaction is what used to fail the whole calendar with a 500.
 */
@ApplicationService
public class ListTasksDueBetweenHandler implements ListTasksDueBetweenUseCase {

    private final TaskRepository taskRepository;

    public ListTasksDueBetweenHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> list(SpaceMembership caller, LocalDate from, LocalDate to) {
        return taskRepository.findBySpaceIdAndDueDateBetween(caller.spaceId(), from, to);
    }
}
