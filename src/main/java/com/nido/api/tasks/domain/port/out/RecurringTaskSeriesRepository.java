package com.nido.api.tasks.domain.port.out;

import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;

import java.util.Optional;
import java.util.UUID;

public interface RecurringTaskSeriesRepository {
    Optional<RecurringTaskSeries> findById(UUID seriesId);
    RecurringTaskSeries create(CreateRecurringTaskSeriesCommand command);
    RecurringTaskSeries advance(UUID seriesId, int nextRotationIndex, int nextOccurrenceCount);
    void deleteById(UUID seriesId);
}
