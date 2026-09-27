package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.MaterializeDueRecurringTasksUseCase;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@ApplicationService
public class MaterializeDueRecurringTasksHandler implements MaterializeDueRecurringTasksUseCase {

    private final TaskRepository taskRepository;
    private final RecurringTaskSeriesRepository seriesRepository;
    private final GetSpaceTodayUseCase spaceToday;

    public MaterializeDueRecurringTasksHandler(TaskRepository taskRepository, RecurringTaskSeriesRepository seriesRepository,
                                               GetSpaceTodayUseCase spaceToday) {
        this.taskRepository = taskRepository;
        this.seriesRepository = seriesRepository;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional
    public void materialize(SpaceMembership caller) {
        materialize(caller, spaceToday.today(caller.spaceId()));
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests materialize deterministically.
     * Annotated in its own right so a caller reaching it through the Spring proxy still gets the
     * transaction boundary the advisory lock in {@code RecurringTaskSeriesMaterializer} depends on.
     */
    @Transactional
    void materialize(SpaceMembership caller, LocalDate today) {
        RecurringTaskSeriesMaterializer.materializeDueOccurrences(taskRepository, seriesRepository, caller.spaceId(), today);
    }
}
