package com.nido.api.calendar.domain.model;

import java.time.ZoneId;
import java.util.List;

/**
 * What an export or a subscription writes: the space's name and zone, its own events in full — each series with its
 * cancellations and its occurrences edited on their own —, and the occurrences of the other sources chosen, on a window.
 */
public record CalendarFeed(String name, ZoneId zone, List<CalendarEvent> events, List<FeedSeries> series,
                           List<CalendarOccurrence> others) {
    public CalendarFeed {
        events = List.copyOf(events);
        series = List.copyOf(series);
        others = List.copyOf(others);
    }
}
