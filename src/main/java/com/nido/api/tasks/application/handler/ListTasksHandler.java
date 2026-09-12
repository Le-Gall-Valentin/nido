package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.tasks.application.port.in.ListTasksUseCase;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskOrdering;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@ApplicationService
public class ListTasksHandler implements ListTasksUseCase {

    private final GetSpaceTodayUseCase spaceToday;
    private final TaskRepository taskRepository;
    private final RecurringTaskSeriesRepository seriesRepository;

    public ListTasksHandler(TaskRepository taskRepository, RecurringTaskSeriesRepository seriesRepository,
                            GetSpaceTodayUseCase spaceToday) {
        this.taskRepository = taskRepository;
        this.seriesRepository = seriesRepository;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional
    public List<Task> list(SpaceMembership caller) {
        return list(caller, spaceToday.today(caller.spaceId()));
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests materialize deterministically.
     * Annotated in its own right (not just via the public overload) so an external caller invoking
     * it directly through the Spring proxy — e.g. an integration test — still gets the transaction
     * boundary the advisory lock in {@code RecurringTaskSeriesMaterializer} depends on.
     */
    @Transactional
    List<Task> list(SpaceMembership caller, LocalDate today) {
        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, caller.spaceId(), today);
        return TaskOrdering.sort(taskRepository.findBySpaceId(caller.spaceId()));
    }
}
