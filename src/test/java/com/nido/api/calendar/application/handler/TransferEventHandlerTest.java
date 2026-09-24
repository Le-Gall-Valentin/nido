package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.application.port.in.DeleteEventUseCase;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferEventHandlerTest {

    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final ResolveMembershipUseCase resolveMembership = mock(ResolveMembershipUseCase.class);
    private final DeleteEventUseCase deleteEvent = mock(DeleteEventUseCase.class);

    private final CalendarSpaceMemberValidator participants = mock(CalendarSpaceMemberValidator.class);
    private final CopyEventHandler copy = new CopyEventHandler(events, resolveMembership, participants);
    private final MoveEventHandler move = new MoveEventHandler(events, resolveMembership, deleteEvent, participants);

    private final UUID spaceId = UUID.randomUUID();
    private final UUID destinationSpaceId = UUID.randomUUID();
    private final UUID callerUserId = UUID.randomUUID();
    private final SpaceMembership member =
        new SpaceMembership(UUID.randomUUID(), spaceId, callerUserId, SpaceRole.MEMBER, Instant.now());
    private final SpaceMembership viewer =
        new SpaceMembership(UUID.randomUUID(), spaceId, callerUserId, SpaceRole.VIEWER, Instant.now());
    private final SpaceMembership memberOfDestination =
        new SpaceMembership(UUID.randomUUID(), destinationSpaceId, callerUserId, SpaceRole.MEMBER, Instant.now());

    @Test
    void aCopiedEventArrivesWithNoParticipants() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, List.of(UUID.randomUUID()), null, null)));
        when(resolveMembership.resolve(destinationSpaceId, callerUserId)).thenReturn(memberOfDestination);

        copy.copy(eventId, destinationSpaceId, member);

        ArgumentCaptor<CreateEventCommand> created = ArgumentCaptor.forClass(CreateEventCommand.class);
        verify(events).create(created.capture());
        // Participants belong to the source space's membership; the destination has other members.
        assertThat(created.getValue().participantIds()).isEmpty();
        assertThat(created.getValue().spaceId()).isEqualTo(destinationSpaceId);
    }

    @Test
    void aCopiedDetachedOccurrenceLosesItsSeriesLink() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(
            Optional.of(event(eventId, List.of(), UUID.randomUUID(), LocalDate.of(2026, 1, 13))));
        when(resolveMembership.resolve(destinationSpaceId, callerUserId)).thenReturn(memberOfDestination);

        copy.copy(eventId, destinationSpaceId, member);

        ArgumentCaptor<CreateEventCommand> created = ArgumentCaptor.forClass(CreateEventCommand.class);
        verify(events).create(created.capture());
        // The destination has no such series, so the link would dangle.
        assertThat(created.getValue().recurringSeriesId()).isNull();
        assertThat(created.getValue().recurringOriginalDate()).isNull();
    }

    @Test
    void aViewerMayCopy() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, List.of(), null, null)));
        when(resolveMembership.resolve(destinationSpaceId, callerUserId)).thenReturn(memberOfDestination);

        copy.copy(eventId, destinationSpaceId, viewer);

        verify(events).create(any());
    }

    @Test
    void aViewerMayNotMove() {
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> move.move(eventId, destinationSpaceId, viewer))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(events, never()).create(any());
    }

    @Test
    void movingDeletesTheSourceThroughTheDeleteUseCaseSoTheExclusionRuleStillApplies() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(
            Optional.of(event(eventId, List.of(), UUID.randomUUID(), LocalDate.of(2026, 1, 13))));
        when(resolveMembership.resolve(destinationSpaceId, callerUserId)).thenReturn(memberOfDestination);

        move.move(eventId, destinationSpaceId, member);

        verify(events).create(any());
        verify(deleteEvent).delete(eventId, member);
        verify(events, never()).delete(any());
    }

    @Test
    void refusesADestinationEqualToTheSource() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId, List.of(), null, null)));

        assertThatThrownBy(() -> copy.copy(eventId, spaceId, member))
            .isInstanceOf(CalendarException.SameSpaceTransfer.class);
    }

    @Test
    void refusesAnEventOfAnotherSpace() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(new CalendarEvent(
            eventId, UUID.randomUUID(), "x", null, null, true,
            LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 1), null,
            null, List.of(), null, null, UUID.randomUUID(), Instant.now())));

        assertThatThrownBy(() -> copy.copy(eventId, destinationSpaceId, member))
            .isInstanceOf(CalendarException.EventNotFound.class);
    }

    private CalendarEvent event(UUID id, List<UUID> participants, UUID seriesId, LocalDate originalDate) {
        return new CalendarEvent(id, spaceId, "Piano", "notes", "salle 2", true,
            LocalDate.of(2026, 1, 13), null, LocalDate.of(2026, 1, 13), null,
            "accent", participants, seriesId, originalDate, callerUserId, Instant.now());
    }
}
