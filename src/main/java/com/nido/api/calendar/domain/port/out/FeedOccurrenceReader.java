package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.FeedToShow;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

/** Reads the calendars a space follows into what a calendar read shows. */
public interface FeedOccurrenceReader {

    /**
     * The occurrences of {@code feeds} overlapping {@code [from, to]}, in {@code zone}. A calendar's file is asked of
     * {@code content} only when it is not in memory already; one that cannot be read is left out, and the rest shown.
     */
    List<CalendarOccurrence> occurrencesOf(List<FeedToShow> feeds, Function<UUID, String> content, ZoneId zone,
                                           Locale locale, LocalDate from, LocalDate to);
}
