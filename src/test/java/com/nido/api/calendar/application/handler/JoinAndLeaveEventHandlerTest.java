package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JoinAndLeaveEventHandlerTest {

    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final CalendarSpaceMemberValidator participants = mock(CalendarSpaceMemberValidator.class);
    private final JoinEventHandler join = new JoinEventHandler(events, participants);
    private final LeaveEventHandler leave = new LeaveEventHandler(events, participants);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @Test
    void joiningAddsTheCallerAndNobodyElse() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, spaceId)));

        join.join(eventId, caller);

        verify(events).addParticipant(eventId, caller.userId());
    }

    @Test
    void leavingRemovesTheCallerAndNobodyElse() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, spaceId)));

        leave.leave(eventId, caller);

        verify(events).removeParticipant(eventId, caller.userId());
    }

    @Test
    void neitherReachesAnEventOfAnotherSpace() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, UUID.randomUUID())));

        assertThatThrownBy(() -> join.join(eventId, caller)).isInstanceOf(CalendarException.EventNotFound.class);
        assertThatThrownBy(() -> leave.leave(eventId, caller)).isInstanceOf(CalendarException.EventNotFound.class);
    }

    private CalendarEvent event(UUID id, UUID space) {
        return new CalendarEvent(id, space, "Apéro", null, null, true,
            LocalDate.of(2026, 3, 2), null, LocalDate.of(2026, 3, 2), null,
            null, List.of(), null, null, UUID.randomUUID(), Instant.now());
    }
}
