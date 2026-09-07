package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.UpdateRecurringTaskSeriesUseCase;
import com.nido.api.tasks.domain.model.RecurrenceScheduler;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@ApplicationService
public class UpdateRecurringTaskSeriesHandler implements UpdateRecurringTaskSeriesUseCase {

    private final RecurringTaskSeriesRepository seriesRepository;

    public UpdateRecurringTaskSeriesHandler(RecurringTaskSeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional
    public RecurringTaskSeries update(UpdateRecurringTaskSeriesCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        LocalDate leadDate = RecurrenceScheduler.nextDueDate(command.anchorDate(), command.leadIntervalType(), command.leadIntervalCount(), 1);
        LocalDate mainDate = RecurrenceScheduler.nextDueDate(command.anchorDate(), command.intervalType(), command.intervalCount(), 1);
        if (leadDate.isAfter(mainDate)) {
            throw new TaskException.LeadTimeExceedsInterval();
        }
        if (command.endDate() != null && command.endDate().isBefore(command.anchorDate())) {
            throw new TaskException.InvalidEndDate();
        }
        RecurringTaskSeries existing = seriesRepository.findById(command.seriesId())
            .orElseThrow(TaskException.RecurringSeriesNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new TaskException.RecurringSeriesNotFound();
        }
        return seriesRepository.update(command);
    }
}
