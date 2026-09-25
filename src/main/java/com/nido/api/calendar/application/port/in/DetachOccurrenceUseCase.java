package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Edits one occurrence of a series without touching the rest. Idempotent: the (series, slot) pair
 * is upserted, so replaying the same request yields the same state rather than a second event.
 *
 * <p>The {@code content} command carries no event id — the slot decides which row is written.
 */
public interface DetachOccurrenceUseCase {
    CalendarEvent detach(UUID seriesId, LocalDate originalDate, UpdateEventCommand content, SpaceMembership caller);
}
