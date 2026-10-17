package com.nido.api.calendar.domain.model;

import java.util.List;

/**
 * What one UID of a file becomes: a series and its edited occurrences, and events of their own — the occurrences of a
 * rule nido cannot say ({@code unfolded}), or a first day off its rule. {@code uid} is null for an event that had none.
 */
public record ImportGroup(String uid, List<ImportedEvent> events, ImportedSeries series, boolean unfolded,
                          int unfoldedOccurrences) {
    public ImportGroup {
        events = List.copyOf(events);
    }

    /**
     * Every row creating the group writes: its events, its series, the occurrences of the series edited on their own and
     * the slots it cancelled. What the 10 000 rows of an import are counted in.
     */
    public int rows() {
        return events.size() + (series == null ? 0 : 1 + series.editedOccurrences().size() + series.cancelledSlots().size());
    }
}
