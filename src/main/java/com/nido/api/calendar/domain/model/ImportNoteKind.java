package com.nido.api.calendar.domain.model;

/** What the report of an import says deserves a look. */
public enum ImportNoteKind {
    /** A rule nido cannot say, its occurrences made events of their own; detail: how many. */
    UNFOLDED,
    /** The same, some occurrences left out — before a year ago, after two years, beyond 500; detail: how many kept. */
    UNFOLDED_PARTIAL,
    /** A text cut to what nido keeps; detail: "title", "description" or "location". */
    TRUNCATED_TEXT,
    /** Times converted from another zone; detail: the zone. */
    CONVERTED_ZONE,
    /** A zone nobody knows, its times taken as the space's; detail: the zone. */
    UNKNOWN_ZONE,
    /** Not imported; detail: VTODO, VJOURNAL, VFREEBUSY, NO_START, CANCELLED, TOO_LONG. */
    IGNORED
}
