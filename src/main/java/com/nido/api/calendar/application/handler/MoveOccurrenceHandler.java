package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.CopyOccurrenceUseCase;
import com.nido.api.calendar.application.port.in.ExcludeOccurrenceUseCase;
import com.nido.api.calendar.application.port.in.MoveOccurrenceUseCase;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Moving one occurrence of a series into another space: a copy there, then the slot cancelled here — in
 * one transaction, so it is never in both nor in neither. Write access is needed on both sides. The
 * cancellation goes through {@link ExcludeOccurrenceUseCase}, which also takes an edited instance of the
 * slot with it: that rule lives in one place.
 */
@ApplicationService
public class MoveOccurrenceHandler implements MoveOccurrenceUseCase {

    private final CopyOccurrenceUseCase copyOccurrence;
    private final ExcludeOccurrenceUseCase excludeOccurrence;

    public MoveOccurrenceHandler(CopyOccurrenceUseCase copyOccurrence, ExcludeOccurrenceUseCase excludeOccurrence) {
        this.copyOccurrence = copyOccurrence;
        this.excludeOccurrence = excludeOccurrence;
    }

    @Override
    @Transactional
    public CalendarEvent move(UUID seriesId, LocalDate originalDate, UUID destinationSpaceId, SpaceMembership caller) {
        caller.ensureCanWrite();
        CalendarEvent created = copyOccurrence.copy(seriesId, originalDate, destinationSpaceId, caller);
        excludeOccurrence.exclude(seriesId, originalDate, caller);
        return created;
    }
}
