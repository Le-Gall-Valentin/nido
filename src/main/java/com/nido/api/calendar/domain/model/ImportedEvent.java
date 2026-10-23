package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** An event as an iCalendar file holds it, placed in the space's zone and ready to be created. */
public record ImportedEvent(String title, String description, String location, boolean allDay,
                           LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime) {}
