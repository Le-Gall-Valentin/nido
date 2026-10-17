package com.nido.api.calendar.domain.model;

import java.util.List;

/** Everything a file holds that nido can keep, what it set aside, and what deserves a look. */
public record ImportReading(List<ImportGroup> groups, int ignored, List<ImportNote> notes) {
    public ImportReading {
        groups = List.copyOf(groups);
        notes = List.copyOf(notes);
    }
}
