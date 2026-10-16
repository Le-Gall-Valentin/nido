package com.nido.api.calendar.domain.model;

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
}
