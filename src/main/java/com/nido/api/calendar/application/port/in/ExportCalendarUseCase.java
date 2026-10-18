package com.nido.api.calendar.application.port.in;

import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.IcalDocument;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.Collection;

public interface ExportCalendarUseCase {
    IcalDocument export(Collection<CalendarSourceType> sources, boolean onlyMine, SpaceMembership caller);
}
