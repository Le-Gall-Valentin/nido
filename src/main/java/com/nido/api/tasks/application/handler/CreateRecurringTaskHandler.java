package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.CreateRecurringTaskUseCase;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
public class CreateRecurringTaskHandler implements CreateRecurringTaskUseCase {

    private final TaskRepository taskRepository;
    private final RecurringTaskSeriesRepository seriesRepository;

    public CreateRecurringTaskHandler(TaskRepository taskRepository, RecurringTaskSeriesRepository seriesRepository) {
        this.taskRepository = taskRepository;
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional
    public Task create(CreateRecurringTaskSeriesCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        RecurringTaskSeries series = seriesRepository.create(command);
        List<UUID> firstAssignees = series.rotationMemberIds().isEmpty()
            ? List.of() : List.of(series.rotationMemberIds().get(0));
        List<SubtaskInput> subtasks = series.subtaskTemplates().stream()
            .map(text -> new SubtaskInput(text, false)).toList();
        return taskRepository.create(new CreateTaskCommand(
            command.spaceId(), command.title(), command.priority(), command.anchorDate(),
            firstAssignees, subtasks, series.id()));
    }
}
