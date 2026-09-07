package com.nido.api.tasks.domain.port.out;

import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTaskSeriesRepository {
    Optional<RecurringTaskSeries> findById(UUID seriesId);
    List<RecurringTaskSeries> findBySpaceId(UUID spaceId);
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
