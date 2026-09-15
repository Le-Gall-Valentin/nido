package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CreateEventHandlerTest {

    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final CalendarSpaceMemberValidator members = mock(CalendarSpaceMemberValidator.class);
    private final CreateEventHandler handler = new CreateEventHandler(events, members);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @Test
    void refusesATimedEventWhoseEndPrecedesItsStartOnTheSameDay() {
        assertThatThrownBy(() -> handler.create(timed(
            LocalDate.of(2026, 3, 2), LocalTime.of(18, 0), LocalDate.of(2026, 3, 2), LocalTime.of(9, 0)), caller))
            .isInstanceOf(CalendarException.InvalidTimeRange.class);
        verify(events, never()).create(any());
    }

    @Test
    void acceptsAnOvernightEventWhoseEndTimeLooksEarlier() {
        // 22:00 on the 2nd to 02:00 on the 3rd is ordinary, and must not be read as inverted.
        assertThatCode(() -> handler.create(timed(
            LocalDate.of(2026, 3, 2), LocalTime.of(22, 0), LocalDate.of(2026, 3, 3), LocalTime.of(2, 0)), caller))
            .doesNotThrowAnyException();
    }

    @Test
    void refusesAnAllDayEventCarryingTimes() {
        assertThatThrownBy(() -> handler.create(new CreateEventCommand(
            spaceId, "x", null, null, true,
            LocalDate.of(2026, 3, 2), LocalTime.of(9, 0), LocalDate.of(2026, 3, 2), LocalTime.of(10, 0),
            null, List.of(), null, null, caller.userId()), caller))
            .isInstanceOf(CalendarException.InvalidTimeRange.class);
    }

    @Test
    void refusesAnEventForAnotherSpaceThanTheOneTheCallerProved() {
        assertThatThrownBy(() -> handler.create(new CreateEventCommand(
            UUID.randomUUID(), "x", null, null, true,
            LocalDate.of(2026, 3, 2), null, LocalDate.of(2026, 3, 2), null,
            null, List.of(), null, null, caller.userId()), caller))
            .isInstanceOf(SpaceException.NotAMember.class);
        verify(events, never()).create(any());
    }

    @Test
    void refusesAParticipantWhoIsNotAMemberOfTheSpace() {
        doThrow(new CalendarException.MemberNotInSpace()).when(members).ensureMembers(any(), any());

        assertThatThrownBy(() -> handler.create(new CreateEventCommand(
            spaceId, "x", null, null, true,
            LocalDate.of(2026, 3, 2), null, LocalDate.of(2026, 3, 2), null,
            null, List.of(UUID.randomUUID()), null, null, caller.userId()), caller))
            .isInstanceOf(CalendarException.MemberNotInSpace.class);
        verify(events, never()).create(any());
    }

    @Test
    void acceptsAMultiDayAllDayEvent() {
        assertThatCode(() -> handler.create(new CreateEventCommand(
            spaceId, "Vacances", null, null, true,
            LocalDate.of(2026, 7, 1), null, LocalDate.of(2026, 7, 20), null,
            null, List.of(), null, null, caller.userId()), caller))
            .doesNotThrowAnyException();
    }

    private CreateEventCommand timed(LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime) {
        return new CreateEventCommand(spaceId, "x", null, null, false,
            startDate, startTime, endDate, endTime, null, List.of(), null, null, caller.userId());
    }
}
