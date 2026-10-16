package com.nido.api.calendar.domain.model;

import java.time.LocalDate;

/** An iCalendar document written for someone: the calendar's name and the space's day name the file. */
public record IcalDocument(String calendarName, LocalDate day, String content) {}
