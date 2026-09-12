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
        return update(command, caller, LocalDate.now());
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests exercise the backlog
     * ceiling against fixed dates instead of whenever the suite happens to run.
     */
    @Transactional
    RecurringTaskSeries update(UpdateRecurringTaskSeriesCommand command, SpaceMembership caller, LocalDate today) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        RecurrenceScheduler.validateSchedule(command.anchorDate(), command.intervalType(), command.intervalCount(),
            command.leadIntervalType(), command.leadIntervalCount(), command.endDate());
        command.rotationMemberIds().forEach(memberId -> spaceMemberValidator.ensureMember(command.spaceId(), memberId));
        RecurringTaskSeries existing = seriesRepository.findById(command.seriesId())
            .orElseThrow(TaskException.RecurringSeriesNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new TaskException.RecurringSeriesNotFound();
        }
        boolean frequencyChanged = existing.intervalType() != command.intervalType()
            || existing.intervalCount() != command.intervalCount();
        if (!frequencyChanged) {
            // Anchor and occurrence count both survive this branch, so the backlog is measured
            // against what the series has already generated — renaming a long-running series
            // owes nothing, moving its anchor into the distant past owes everything.
            RecurrenceScheduler.validateBacklog(command.anchorDate(), command.intervalType(),
                command.intervalCount(), command.endDate(), today, existing.occurrenceCount());
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
        // Validated against the re-anchored command, not the submitted one: this branch discards
        // command.anchorDate() entirely, and resets the occurrence count to 0 along with it.
        RecurrenceScheduler.validateBacklog(lastGeneratedDueDate, command.intervalType(),
            command.intervalCount(), command.endDate(), today, 0);
        seriesRepository.update(reAnchoredCommand);
        return seriesRepository.advance(command.seriesId(), existing.currentRotationIndex(), 0);
    }
}
