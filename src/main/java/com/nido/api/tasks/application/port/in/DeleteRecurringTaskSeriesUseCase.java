package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteRecurringTaskSeriesUseCase {
    void delete(UUID seriesId, UUID spaceId, SpaceMembership caller);
}
