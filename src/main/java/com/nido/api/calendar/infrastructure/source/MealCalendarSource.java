package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.kitchen.application.port.in.ListMenuEntriesUseCase;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Planned meals. The kitchen's menu read is already windowed, so this is a straight mapping. */
@Component
public class MealCalendarSource implements CalendarSource {

    private final ListMenuEntriesUseCase listMenuEntriesUseCase;

    public MealCalendarSource(ListMenuEntriesUseCase listMenuEntriesUseCase) {
        this.listMenuEntriesUseCase = listMenuEntriesUseCase;
    }

    @Override
    public CalendarSourceType type() {
        return CalendarSourceType.MEAL;
    }

    @Override
    public List<CalendarOccurrence> occurrencesBetween(SpaceMembership caller, LocalDate from, LocalDate to) {
        return listMenuEntriesUseCase.list(caller, from, to).stream()
            .map(MealCalendarSource::toOccurrence)
            .toList();
    }

    private static CalendarOccurrence toOccurrence(MenuEntryView view) {
        LocalDate date = view.entry().date();
        // A menu entry whose recipe was deleted still has a row; naming it after the missing recipe
        // would be a null title, which the record refuses. Fall back rather than fail the read.
        String title = view.recipe() == null ? "—" : view.recipe().name();
        return new CalendarOccurrence(
            CalendarSourceType.MEAL, view.entry().id().toString(), null, null, true,
            title, null, null, true, date, null, date, null, null, List.of());
    }
}
