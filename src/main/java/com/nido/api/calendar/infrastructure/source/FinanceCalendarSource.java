package com.nido.api.calendar.infrastructure.source;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.finance.application.port.in.ListTransactionsInRangeUseCase;
import com.nido.api.finance.application.port.in.ProjectRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Recurring money: the transactions a series already produced, and the ones it will.
 *
 * <p>Only transactions tied to a series appear. A calendar of every one-off purchase would drown
 * the household's actual schedule, and the finance page is where that list belongs.
 */
@Component
public class FinanceCalendarSource implements CalendarSource {

    private final ListTransactionsInRangeUseCase listTransactionsUseCase;
    private final ProjectRecurringSeriesUseCase projectRecurringSeriesUseCase;

    public FinanceCalendarSource(ListTransactionsInRangeUseCase listTransactionsUseCase,
                                 ProjectRecurringSeriesUseCase projectRecurringSeriesUseCase) {
        this.listTransactionsUseCase = listTransactionsUseCase;
        this.projectRecurringSeriesUseCase = projectRecurringSeriesUseCase;
    }

    @Override
    public CalendarSourceType type() {
        return CalendarSourceType.FINANCE;
    }

    @Override
    public List<CalendarOccurrence> occurrencesBetween(SpaceMembership caller, LocalDate from, LocalDate to) {
        List<CalendarOccurrence> produced = new ArrayList<>();
        for (Transaction transaction : listTransactionsUseCase.list(caller, from, to)) {
            if (transaction.recurringSeriesId() == null) {
                continue;
            }
            produced.add(new CalendarOccurrence(
                CalendarSourceType.FINANCE, transaction.id().toString(),
                transaction.recurringSeriesId(), transaction.date(), true,
                transaction.label(), true,
                transaction.date(), null, transaction.date(), null, null, List.of()));
        }
        for (ProjectedOccurrence projected : projectRecurringSeriesUseCase.project(caller, from, to)) {
            produced.add(new CalendarOccurrence(
                CalendarSourceType.FINANCE,
                CalendarOccurrence.projectedId(projected.seriesId(), projected.date()),
                projected.seriesId(), projected.date(), false,
                projected.label(), true,
                projected.date(), null, projected.date(), null, null, List.of()));
        }
        return produced;
    }
}
