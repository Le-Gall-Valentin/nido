package com.nido.api.calendar.domain.model;

import java.time.DayOfWeek;
import java.util.List;

/**
 * The parts of an iCalendar RRULE that nido may be able to say, as the import reads them — free of any library, so the
 * translation into a nido rule is plain domain code. {@code hasOtherParts} is true when the rule uses anything else
 * (BYHOUR, BYMINUTE, BYSECOND, BYYEARDAY, BYWEEKNO): nothing nido can say. COUNT and UNTIL are read apart, by the import.
 */
public record IcalRecurrence(String frequency, int interval, List<ByDay> byDay, List<Integer> byMonthDay,
                             List<Integer> byMonth, List<Integer> bySetPos, boolean hasOtherParts) {

    /** {@code offset} 0 means every such day of the period; 2 the second; -1 the last. */
    public record ByDay(int offset, DayOfWeek day) {}

    public IcalRecurrence {
        byDay = List.copyOf(byDay);
        byMonthDay = List.copyOf(byMonthDay);
        byMonth = List.copyOf(byMonth);
        bySetPos = List.copyOf(bySetPos);
    }
}
