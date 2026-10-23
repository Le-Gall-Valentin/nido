package com.nido.api.calendar.domain.model;

import java.util.List;

/**
 * Everything a file holds that nido can keep, the UIDs it skipped as already in the space ({@code alreadyImported}), what
 * it set aside, and what deserves a look — of the events it read, never of those it skipped.
 */
public record ImportReading(List<ImportGroup> groups, int alreadyImported, int ignored, List<ImportNote> notes) {
    public ImportReading {
        groups = List.copyOf(groups);
        notes = List.copyOf(notes);
    }
}
