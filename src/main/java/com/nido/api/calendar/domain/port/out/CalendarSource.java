package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;

/**
 * One contributor to the unified feed.
 *
 * <p>Every implementation lives inside this bounded context, in {@code infrastructure/source/},
 * and calls the inbound use case of the context it reads. The dependency therefore runs one way
 * only — calendar depends on tasks, finance and kitchen, and none of them ever learns the calendar
 * exists. Adding a sixth source is one adapter and one enum constant; no existing context changes.
 */
public interface CalendarSource {

    CalendarSourceType type();

    List<CalendarOccurrence> occurrencesBetween(SpaceMembership caller, LocalDate from, LocalDate to);
}
