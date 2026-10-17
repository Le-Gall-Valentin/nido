package com.nido.api.calendar.domain.model;

import java.util.List;

/** What an import did — or, for an analysis ({@code written} false), what it would do. */
public record ImportReport(int eventsCreated, int seriesCreated, int seriesUnfolded, int occurrencesUnfolded,
                           int duplicates, int ignored, List<ImportNote> notes, boolean written) {
    public ImportReport {
        notes = List.copyOf(notes);
    }
}
