package com.nido.api.calendar.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.UUID;

/** Cancels one occurrence of a series, leaving every other occurrence alone. */
public interface ExcludeOccurrenceUseCase {
    void exclude(UUID seriesId, LocalDate originalDate, SpaceMembership caller);
}
