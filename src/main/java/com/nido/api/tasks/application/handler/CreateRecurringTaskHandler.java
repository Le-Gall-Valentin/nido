package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.tasks.application.port.in.CreateRecurringTaskUseCase;
import com.nido.api.tasks.application.service.TaskSpaceMemberValidator;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceScheduler;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationService
public class CreateRecurringTaskHandler implements CreateRecurringTaskUseCase {

    private final GetSpaceTodayUseCase spaceToday;
    private final TaskRepository taskRepository;
    private final RecurringTaskSeriesRepository seriesRepository;
    private final TaskSpaceMemberValidator spaceMemberValidator;

    public CreateRecurringTaskHandler(TaskRepository taskRepository, RecurringTaskSeriesRepository seriesRepository,
                                       TaskSpaceMemberValidator spaceMemberValidator,
                            GetSpaceTodayUseCase spaceToday) {
        this.taskRepository = taskRepository;
        this.seriesRepository = seriesRepository;
        this.spaceMemberValidator = spaceMemberValidator;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional
    public Task create(CreateRecurringTaskSeriesCommand command, SpaceMembership caller) {
        return create(command, caller, spaceToday.today(caller.spaceId()));
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests exercise the backlog
     * ceiling against fixed dates instead of whenever the suite happens to run.
     */
    @Transactional
    Task create(CreateRecurringTaskSeriesCommand command, SpaceMembership caller, LocalDate today) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        RecurrenceScheduler.validateSchedule(command.anchorDate(), command.intervalType(), command.intervalCount(),
            command.leadIntervalType(), command.leadIntervalCount(), command.endDate());
        // A brand-new series has generated nothing yet, so its whole past is backlog.
        RecurrenceScheduler.validateBacklog(command.anchorDate(), command.intervalType(),
            command.intervalCount(), command.endDate(), today, 0);
        command.rotationMemberIds().forEach(memberId -> spaceMemberValidator.ensureMember(command.spaceId(), memberId));
        RecurringTaskSeries series = seriesRepository.create(command);
        List<UUID> firstAssignees = series.rotationMemberIds().isEmpty()
            ? List.of() : List.of(series.rotationMemberIds().get(0));
        List<SubtaskInput> subtasks = series.subtaskTemplates().stream()
            .map(text -> new SubtaskInput(text, false)).toList();
        return taskRepository.create(new CreateTaskCommand(
            command.spaceId(), command.title(), command.priority(), command.anchorDate(),
            firstAssignees, subtasks, series.id(), series.createdBy()));
    }
}
