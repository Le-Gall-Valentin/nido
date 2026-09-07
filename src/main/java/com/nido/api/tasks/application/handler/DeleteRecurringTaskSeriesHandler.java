package com.nido.api.tasks.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.DeleteRecurringTaskSeriesUseCase;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteRecurringTaskSeriesHandler implements DeleteRecurringTaskSeriesUseCase {

    private final RecurringTaskSeriesRepository seriesRepository;

    public DeleteRecurringTaskSeriesHandler(RecurringTaskSeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional
    public void delete(UUID seriesId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        // Same lock RecurringTaskSeriesMaterializer takes: without it, a concurrent lazy
        // materialization could read this series and insert an occurrence for it in the
        // window between our findById and our delete.
        seriesRepository.lockForMaterialization(spaceId);
        RecurringTaskSeries existing = seriesRepository.findById(seriesId).orElseThrow(TaskException.RecurringSeriesNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new TaskException.RecurringSeriesNotFound();
        }
        seriesRepository.deleteById(seriesId);
    }
}
