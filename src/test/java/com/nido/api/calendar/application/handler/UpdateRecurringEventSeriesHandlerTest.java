package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.application.service.SeriesExceptionsMover;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.DayRule;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UpdateRecurringEventSeriesHandlerTest {

    /** The household's today: Thursday 22 October 2026. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private final RecurringEventSeriesRepository series = mock(RecurringEventSeriesRepository.class);
    private final CalendarSpaceMemberValidator members = mock(CalendarSpaceMemberValidator.class);
    private final GetSpaceTodayUseCase spaceToday = mock(GetSpaceTodayUseCase.class);
    private final SeriesExceptionsMover exceptions = mock(SeriesExceptionsMover.class);
    private final UpdateRecurringEventSeriesHandler handler =
        new UpdateRecurringEventSeriesHandler(series, members, spaceToday, exceptions);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @BeforeEach
    void setUp() {
        when(spaceToday.today(spaceId)).thenReturn(TODAY);
        when(members.participantsFor(any(), anyList(), any())).thenReturn(List.of());
    }

    @Test
    void aSeriesNotBegunYetIsEditedWholeOnTheWeekdaysGiven() {
        // Tuesdays from 3 November; now Mondays and Thursdays from 2 November.
        RecurringEventSeries existing = stored(RecurrenceInterval.WEEKLY, LocalDate.of(2026, 11, 3));
        RecurringEventSeries updated = stored(RecurrenceInterval.WEEKLY, LocalDate.of(2026, 11, 2));
        when(series.update(any())).thenReturn(updated);

        handler.update(edit(existing, RecurrenceInterval.WEEKLY, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null,
            LocalDate.of(2026, 11, 2)), caller);

        ArgumentCaptor<UpdateRecurringEventSeriesCommand> written = ArgumentCaptor.forClass(UpdateRecurringEventSeriesCommand.class);
        verify(series).update(written.capture());
        assertThat(written.getValue().weekdays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
        verify(exceptions).carryOver(existing.id(), null, updated);
    }

    @Test
    void aBegunSeriesCarriesOnFromTodayOnTheDayRuleGiven() {
        // The 30th of each month since January; from today on, the last Friday of each month.
        RecurringEventSeries existing = stored(RecurrenceInterval.MONTHLY, LocalDate.of(2026, 1, 30));
        RecurringEventSeries carriedOn = stored(RecurrenceInterval.MONTHLY, LocalDate.of(2026, 1, 30));
        when(series.create(any())).thenReturn(carriedOn);

        handler.update(edit(existing, RecurrenceInterval.MONTHLY, null, DayRule.LAST_WEEKDAY_OF_MONTH,
            LocalDate.of(2026, 1, 30)), caller);

        verify(series).endOn(existing.id(), TODAY.minusDays(1));
        ArgumentCaptor<CreateRecurringEventSeriesCommand> written = ArgumentCaptor.forClass(CreateRecurringEventSeriesCommand.class);
        verify(series).create(written.capture());
        assertThat(written.getValue().dayRule()).isEqualTo(DayRule.LAST_WEEKDAY_OF_MONTH);
        assertThat(written.getValue().startsOn()).isEqualTo(TODAY);
        verify(exceptions).carryOver(existing.id(), TODAY, carriedOn);
    }

    @Test
    void aRuleThatDoesNotFitIsRefusedBeforeAnythingIsWritten() {
        RecurringEventSeries existing = stored(RecurrenceInterval.WEEKLY, LocalDate.of(2026, 1, 6));

        // Mondays and Thursdays, anchored on a Tuesday.
        assertThatThrownBy(() -> handler.update(edit(existing, RecurrenceInterval.WEEKLY,
            Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null, LocalDate.of(2026, 1, 6)), caller))
            .isInstanceOf(CalendarException.InvalidRecurrenceRule.class);
        assertNothingWritten();
    }

    @Test
    void anIntervalOverTheMaximumIsRefusedBeforeAnythingIsWritten() {
        RecurringEventSeries existing = stored(RecurrenceInterval.YEARLY, LocalDate.of(2026, 1, 6));
        UpdateRecurringEventSeriesCommand everyBillionYears = new UpdateRecurringEventSeriesCommand(existing.id(), "Fête",
            null, null, true, null, null, 0, null, RecurrenceInterval.YEARLY, 1_000_000_000, LocalDate.of(2026, 1, 6),
            null, List.of());

        assertThatThrownBy(() -> handler.update(everyBillionYears, caller))
            .isInstanceOf(CalendarException.IntervalTooLarge.class);
        assertNothingWritten();
    }

    private void assertNothingWritten() {
        verify(series, never()).update(any());
        verify(series, never()).create(any());
        verify(series, never()).endOn(any(), any());
        verifyNoInteractions(exceptions);
    }

    private RecurringEventSeries stored(RecurrenceInterval interval, LocalDate anchor) {
        RecurringEventSeries found = new RecurringEventSeries(UUID.randomUUID(), spaceId, "Piano", null, null, true, null,
            null, 0, null, interval, 1, anchor, null, List.of(), UUID.randomUUID(), Instant.now());
        when(series.findById(found.id())).thenReturn(Optional.of(found));
        return found;
    }

    private static UpdateRecurringEventSeriesCommand edit(RecurringEventSeries existing, RecurrenceInterval interval,
                                                          Set<DayOfWeek> weekdays, DayRule dayRule, LocalDate anchor) {
        return new UpdateRecurringEventSeriesCommand(existing.id(), "Piano", null, null, true, null, null, 0, null,
            interval, 1, weekdays, dayRule, anchor, null, List.of());
    }
}
