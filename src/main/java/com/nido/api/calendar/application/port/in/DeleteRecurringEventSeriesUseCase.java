package com.nido.api.calendar.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteRecurringEventSeriesUseCase {
    void delete(UUID seriesId, SpaceMembership caller);
}
