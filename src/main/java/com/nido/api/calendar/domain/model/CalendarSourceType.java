package com.nido.api.calendar.domain.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Where an entry of the unified feed came from. */
public enum CalendarSourceType {
    EVENT, TASK, FINANCE, MEAL, SAVINGS,
    /** A calendar the space follows at an address of its own: read-only, never exported again. */
    EXTERNAL;

    /** What an export or a subscription may write: everything nido owns. */
    public static Set<CalendarSourceType> exportable() {
        return EnumSet.of(EVENT, TASK, FINANCE, MEAL, SAVINGS);
    }

    /** The sources asked for, if each is exportable and there is at least one. */
    public static Set<CalendarSourceType> checkExportable(Collection<CalendarSourceType> asked) {
        if (asked == null || asked.isEmpty() || !exportable().containsAll(asked)) {
            throw new CalendarException.InvalidSources();
        }
        return EnumSet.copyOf(asked);
    }
}
