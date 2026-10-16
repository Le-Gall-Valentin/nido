package com.nido.api.calendar.domain.model;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The days a weekly series repeats on, and their two-letter codes — those of iCalendar ({@code BYDAY=MO,TH}), which is
 * also how the database stores them. An ensemble is always ordered from Monday to Sunday and never changed in place.
 */
public final class Weekdays {

    private static final List<String> CODES = List.of("MO", "TU", "WE", "TH", "FR", "SA", "SU");

    private Weekdays() {}

    public static Set<DayOfWeek> of(DayOfWeek first, DayOfWeek... others) {
        return Collections.unmodifiableSet(EnumSet.of(first, others));
    }

    public static Set<DayOfWeek> copyOf(Collection<DayOfWeek> days) {
        return Collections.unmodifiableSet(EnumSet.copyOf(days));
    }

    public static String code(DayOfWeek day) {
        return CODES.get(day.getValue() - 1);
    }

    public static DayOfWeek fromCode(String code) {
        int index = CODES.indexOf(code);
        if (index < 0) {
            throw new IllegalArgumentException("Not a weekday code: " + code);
        }
        return DayOfWeek.of(index + 1);
    }

    public static String toCodes(Set<DayOfWeek> days) {
        return days == null ? null : days.stream().sorted().map(Weekdays::code).collect(Collectors.joining(","));
    }

    public static Set<DayOfWeek> fromCodes(String codes) {
        return codes == null ? null : copyOf(Arrays.stream(codes.split(",")).map(Weekdays::fromCode).toList());
    }
}
