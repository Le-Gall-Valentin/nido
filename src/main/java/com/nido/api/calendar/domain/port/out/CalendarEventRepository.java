package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.UpdateEventCommand;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CalendarEventRepository {

    Optional<CalendarEvent> findById(UUID eventId);

    /**
     * Every event of the space <em>overlapping</em> the window, not merely starting inside it —
     * a fortnight's holiday must show on a week that only touches its middle.
     */
    List<CalendarEvent> findBySpaceIdOverlapping(UUID spaceId, LocalDate from, LocalDate to);

    /**
     * The slots inside {@code [from, to]} taken over by a detached instance, series by series, in one
     * read — a series with none is absent. Keyed on the slot each replaces, never on where the instance
     * now sits: an occurrence moved out of the window must still free its original slot, or the series
     * re-emits it and the same event appears twice.
     */
    Map<UUID, Set<LocalDate>> findDetachedSlotsOfEach(Collection<UUID> seriesIds, LocalDate from, LocalDate to);

    Optional<CalendarEvent> findBySeriesAndOriginalDate(UUID seriesId, LocalDate originalDate);

    /** Every occurrence of the series edited on its own, wherever each now sits. */
    List<CalendarEvent> findDetachedOf(UUID seriesId);

    /**
     * Ties an edited occurrence to the slot it takes over, in the series given — or to none, when both
     * are null, which leaves it an event of its own. Nothing else about the event changes.
     */
    void relink(UUID eventId, UUID seriesId, LocalDate originalDate);

    CalendarEvent create(CreateEventCommand command);

    CalendarEvent update(UpdateEventCommand command);

    void delete(UUID eventId);

    /** Idempotent: joining an event twice is not an error. */
    void addParticipant(UUID eventId, UUID userId);

    void removeParticipant(UUID eventId, UUID userId);
}
