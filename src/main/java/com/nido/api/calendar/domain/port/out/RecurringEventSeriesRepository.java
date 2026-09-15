package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringEventSeriesRepository {

    Optional<RecurringEventSeries> findById(UUID seriesId);

    List<RecurringEventSeries> findBySpaceId(UUID spaceId);

    RecurringEventSeries create(CreateRecurringEventSeriesCommand command);

    RecurringEventSeries update(UpdateRecurringEventSeriesCommand command);

    /** Exclusions, participants and detached instances go with it, by ON DELETE CASCADE. */
    void delete(UUID seriesId);
}
