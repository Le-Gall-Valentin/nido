package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.IcalDocument;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.Set;

public interface ExportCalendarUseCase {
    IcalDocument export(Set<CalendarSourceType> sources, boolean onlyMine, SpaceMembership caller);
}
