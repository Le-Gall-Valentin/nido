package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.UpdateRecurringTaskSeriesUseCase;
import com.nido.api.tasks.application.service.TaskSpaceMemberValidator;
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
    private final TaskSpaceMemberValidator spaceMemberValidator;

    public UpdateRecurringTaskSeriesHandler(RecurringTaskSeriesRepository seriesRepository, TaskSpaceMemberValidator spaceMemberValidator) {
        this.seriesRepository = seriesRepository;
        this.spaceMemberValidator = spaceMemberValidator;
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
        command.rotationMemberIds().forEach(memberId -> spaceMemberValidator.ensureMember(command.spaceId(), memberId));
        RecurringTaskSeries existing = seriesRepository.findById(command.seriesId())
            .orElseThrow(TaskException.RecurringSeriesNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new TaskException.RecurringSeriesNotFound();
        }
        boolean frequencyChanged = existing.intervalType() != command.intervalType()
            || existing.intervalCount() != command.intervalCount();
        if (!frequencyChanged) {
            return seriesRepository.update(command);
        }
        // Re-anchoring on the last generated occurrence (instead of keeping the original
        // anchor) means the next occurrence under the new frequency is exactly one new
        // interval after the last task actually generated - not a jump computed by applying
        // the new interval occurrenceCount times from the old anchor, which could land
        // decades away. occurrenceCount resets to 0 since the new anchor already accounts
        // for every occurrence generated so far.
        LocalDate lastGeneratedDueDate = RecurrenceScheduler.nextDueDate(
            existing.anchorDate(), existing.intervalType(), existing.intervalCount(), existing.occurrenceCount());
        UpdateRecurringTaskSeriesCommand reAnchoredCommand = new UpdateRecurringTaskSeriesCommand(
            command.seriesId(), command.spaceId(), command.title(), command.priority(), command.subtaskTemplates(),
            command.intervalType(), command.intervalCount(), command.leadIntervalType(), command.leadIntervalCount(),
            lastGeneratedDueDate, command.endDate(), command.rotationMemberIds());
        seriesRepository.update(reAnchoredCommand);
        return seriesRepository.advance(command.seriesId(), existing.currentRotationIndex(), 0);
    }
}
