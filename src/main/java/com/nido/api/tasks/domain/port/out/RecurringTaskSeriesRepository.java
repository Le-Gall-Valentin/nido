package com.nido.api.tasks.domain.port.out;

import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.RecurringTaskSeriesSchedule;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTaskSeriesRepository {
    Optional<RecurringTaskSeries> findById(UUID seriesId);
    List<RecurringTaskSeries> findBySpaceId(UUID spaceId);

    /**
     * Scheduling fields only, for deciding whether a space owes anything before committing to
     * the work of materializing it. Cheap, and safe to call before
     * {@link #lockForMaterialization(UUID)} — see {@link RecurringTaskSeriesSchedule}.
     */
    List<RecurringTaskSeriesSchedule> findSchedulesBySpaceId(UUID spaceId);
    RecurringTaskSeries create(CreateRecurringTaskSeriesCommand command);
    RecurringTaskSeries update(UpdateRecurringTaskSeriesCommand command);
    RecurringTaskSeries advance(UUID seriesId, int nextRotationIndex, int nextOccurrenceCount);
    void deleteById(UUID seriesId);

    /**
     * Serializes concurrent lazy materialization for a space: held until the caller's
     * transaction commits or rolls back, so two requests racing to materialize the same
     * due occurrence can't both read the same occurrenceCount and each insert it — see
     * {@code RecurringTaskSeriesMaterializer}.
     */
    void lockForMaterialization(UUID spaceId);
}
