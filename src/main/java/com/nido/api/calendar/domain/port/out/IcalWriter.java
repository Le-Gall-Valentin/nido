package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.CalendarFeed;

import java.util.Locale;

/** Writes a calendar as an iCalendar (RFC 5545) document; {@code locale} words what the other sources are. */
public interface IcalWriter {
    String write(CalendarFeed feed, Locale locale);
}
