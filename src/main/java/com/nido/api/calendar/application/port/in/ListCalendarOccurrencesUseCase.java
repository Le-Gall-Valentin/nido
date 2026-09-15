package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;

public interface ListCalendarOccurrencesUseCase {
    List<CalendarOccurrence> list(SpaceMembership caller, LocalDate from, LocalDate to);
}
