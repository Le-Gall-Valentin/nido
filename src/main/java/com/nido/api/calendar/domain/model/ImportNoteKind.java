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
    /**
     * Not imported; detail: VTODO, VJOURNAL, VFREEBUSY, NO_START, CANCELLED, TOO_LONG, DUPLICATE_UID (a second event of a
     * UID, or a second version of one of its edited occurrences), TOO_HEAVY (a rule too costly to expand: seconds,
     * minutes, or beyond the budget of the file), BAD_RULE (a rule nido cannot read), MALFORMED (a property nido reads —
     * its start, its end, its rule, its title… — that does not parse: the event and its edited occurrences, never
     * guessed), NIDO_SOURCE (a task, a meal, finance or savings written by a nido export: another part of nido shows it,
     * and it is never imported, in any space).
     */
    IGNORED
}
