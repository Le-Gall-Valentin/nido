package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** A series as it is written: its rule, the slots it no longer shows, and the occurrences edited on their own. */
public record FeedSeries(RecurringEventSeries series, Set<LocalDate> cancelledSlots, List<CalendarEvent> editedOccurrences) {
    public FeedSeries {
        cancelledSlots = Set.copyOf(cancelledSlots);
        editedOccurrences = List.copyOf(editedOccurrences);
    }
}
