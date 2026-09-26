package com.nido.api.dashboard.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** {@code id} is the calendar's opaque source id ("seriesId:date" for a projected occurrence). */
public record AgendaEvent(String id, String title, String location, String color,
                          LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime,
                          List<UUID> participantIds) {
}
