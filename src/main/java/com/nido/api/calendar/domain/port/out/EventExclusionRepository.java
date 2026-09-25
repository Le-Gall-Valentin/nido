package com.nido.api.calendar.domain.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Cancelled occurrences of a series. A cancellation carries no content beyond its slot. */
public interface EventExclusionRepository {

    /** The cancelled slots inside {@code [from, to]} of each series, in one read: a series with none is absent. */
    Map<UUID, Set<LocalDate>> findSlotsOfEach(Collection<UUID> seriesIds, LocalDate from, LocalDate to);

    /** Idempotent: excluding an already-excluded slot is not an error. */
    void exclude(UUID seriesId, LocalDate originalDate);

    /**
     * Lifts an exclusion. Needed when a cancelled occurrence is edited instead: without this,
     * the new detached instance would be created under a slot that is still cancelled, and stay
     * invisible.
     */
    void clear(UUID seriesId, LocalDate originalDate);

    /** Every slot of the series cancelled so far. */
    Set<LocalDate> findAllSlots(UUID seriesId);

    /** Lifts every cancellation of the series from {@code from} on — all of them when it is null. */
    void clearFrom(UUID seriesId, LocalDate from);
}
