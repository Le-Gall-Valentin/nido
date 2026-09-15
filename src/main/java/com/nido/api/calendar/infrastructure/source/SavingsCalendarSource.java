package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.finance.application.port.in.ListSavingsGoalsUseCase;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Savings-goal deadlines: one milestone per goal that has a target date inside the window.
 * Goals without one never appear — an open-ended goal has no day to sit on.
 */
@Component
public class SavingsCalendarSource implements CalendarSource {

    private final ListSavingsGoalsUseCase listSavingsGoalsUseCase;

    public SavingsCalendarSource(ListSavingsGoalsUseCase listSavingsGoalsUseCase) {
        this.listSavingsGoalsUseCase = listSavingsGoalsUseCase;
    }

    @Override
    public CalendarSourceType type() {
        return CalendarSourceType.SAVINGS;
    }

    @Override
    public List<CalendarOccurrence> occurrencesBetween(SpaceMembership caller, LocalDate from, LocalDate to) {
        return listSavingsGoalsUseCase.list(caller).stream()
            .map(SavingsGoalDetail::goal)
            .filter(goal -> goal.targetDate() != null
                && !goal.targetDate().isBefore(from) && !goal.targetDate().isAfter(to))
            .map(SavingsCalendarSource::toOccurrence)
            .toList();
    }

    private static CalendarOccurrence toOccurrence(SavingsGoal goal) {
        LocalDate date = goal.targetDate();
        return new CalendarOccurrence(
            CalendarSourceType.SAVINGS, goal.id().toString(), null, null, true,
            goal.name(), true, date, null, date, null, null, List.of());
    }
}
