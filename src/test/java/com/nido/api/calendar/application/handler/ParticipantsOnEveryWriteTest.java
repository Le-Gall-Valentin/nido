package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.DeleteEventUseCase;
import com.nido.api.calendar.application.service.CalendarSpaceMemberValidator;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Every write stores the participants the rule resolves — in a personal space, its owner — and
 * never the ones the request carried. The rule itself is CalendarSpaceMemberValidatorTest's.
 */
class ParticipantsOnEveryWriteTest {

    private final CalendarEventRepository events = mock(CalendarEventRepository.class);
    private final RecurringEventSeriesRepository series = mock(RecurringEventSeriesRepository.class);
    private final CalendarSpaceMemberValidator rule = mock(CalendarSpaceMemberValidator.class);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership owner =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.OWNER, Instant.now());
    private final List<UUID> asked = List.of(UUID.randomUUID());
    private final List<UUID> resolved = List.of(owner.userId());
    /** Who takes part in what is edited already — handed to the rule, which lets them stay. */
    private final List<UUID> takingPart = List.of(UUID.randomUUID());
    private final LocalDate day = LocalDate.of(2026, 10, 7);

    @Test
    void creatingAnEventStoresTheResolvedParticipants() {
        when(rule.participantsFor(owner, asked)).thenReturn(resolved);

        new CreateEventHandler(events, rule).create(new CreateEventCommand(spaceId, "Dentiste", null, null, true,
            day, null, day, null, null, asked, null, null, owner.userId()), owner);

        ArgumentCaptor<CreateEventCommand> stored = ArgumentCaptor.forClass(CreateEventCommand.class);
        verify(events).create(stored.capture());
        assertThat(stored.getValue().participantIds()).isEqualTo(resolved);
    }

    @Test
    void editingAnEventStoresTheResolvedParticipants() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId)));
        when(rule.participantsFor(owner, asked, takingPart)).thenReturn(resolved);

        new UpdateEventHandler(events, rule).update(new UpdateEventCommand(eventId, "Dentiste", null, null, true,
            day, null, day, null, null, asked), owner);

        ArgumentCaptor<UpdateEventCommand> stored = ArgumentCaptor.forClass(UpdateEventCommand.class);
        verify(events).update(stored.capture());
        assertThat(stored.getValue().participantIds()).isEqualTo(resolved);
    }

    @Test
    void editingOneOccurrenceStoresTheResolvedParticipants() {
        RecurringEventSeries weekly = weekly();
        when(series.findById(weekly.id())).thenReturn(Optional.of(weekly));
        when(events.findBySeriesAndOriginalDate(weekly.id(), day)).thenReturn(Optional.empty());
        when(rule.participantsFor(eq(owner), eq(asked), any())).thenReturn(resolved);

        new DetachOccurrenceHandler(series, events, mock(EventExclusionRepository.class), rule).detach(
            weekly.id(), day, new UpdateEventCommand(null, "Piano", null, null, true, day, null, day, null, null, asked), owner);

        ArgumentCaptor<CreateEventCommand> stored = ArgumentCaptor.forClass(CreateEventCommand.class);
        verify(events).create(stored.capture());
        assertThat(stored.getValue().participantIds()).isEqualTo(resolved);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> alreadyTakingPart = ArgumentCaptor.forClass(Collection.class);
        verify(rule).participantsFor(eq(owner), eq(asked), alreadyTakingPart.capture());
        assertThat(alreadyTakingPart.getValue()).containsExactlyInAnyOrderElementsOf(takingPart);
    }

    @Test
    void editingAnOccurrenceEditedBeforeCountsItsOwnParticipantsAndTheSeriesOnes() {
        RecurringEventSeries weekly = weekly();
        UUID editedAlone = UUID.randomUUID();
        CalendarEvent detached = new CalendarEvent(UUID.randomUUID(), spaceId, "Piano", null, null, true, day, null, day,
            null, null, List.of(editedAlone), weekly.id(), day, owner.userId(), Instant.now());
        when(series.findById(weekly.id())).thenReturn(Optional.of(weekly));
        when(events.findBySeriesAndOriginalDate(weekly.id(), day)).thenReturn(Optional.of(detached));
        when(rule.participantsFor(eq(owner), eq(asked), any())).thenReturn(resolved);

        new DetachOccurrenceHandler(series, events, mock(EventExclusionRepository.class), rule).detach(
            weekly.id(), day, new UpdateEventCommand(null, "Piano", null, null, true, day, null, day, null, null, asked), owner);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> alreadyTakingPart = ArgumentCaptor.forClass(Collection.class);
        verify(rule).participantsFor(eq(owner), eq(asked), alreadyTakingPart.capture());
        assertThat(alreadyTakingPart.getValue()).containsExactlyInAnyOrder(takingPart.get(0), editedAlone);
    }

    @Test
    void creatingASeriesStoresTheResolvedParticipants() {
        when(rule.participantsFor(owner, asked)).thenReturn(resolved);

        new CreateRecurringEventSeriesHandler(series, rule).create(new CreateRecurringEventSeriesCommand(spaceId, "Piano",
            null, null, true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, day, null, asked, owner.userId()), owner);

        ArgumentCaptor<CreateRecurringEventSeriesCommand> stored = ArgumentCaptor.forClass(CreateRecurringEventSeriesCommand.class);
        verify(series).create(stored.capture());
        assertThat(stored.getValue().participantIds()).isEqualTo(resolved);
    }

    @Test
    void editingASeriesStoresTheResolvedParticipants() {
        RecurringEventSeries weekly = weekly();
        when(series.findById(weekly.id())).thenReturn(Optional.of(weekly));
        when(rule.participantsFor(owner, asked, takingPart)).thenReturn(resolved);

        new UpdateRecurringEventSeriesHandler(series, rule).update(new UpdateRecurringEventSeriesCommand(weekly.id(), "Piano",
            null, null, true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, day, null, asked), owner);

        ArgumentCaptor<UpdateRecurringEventSeriesCommand> stored = ArgumentCaptor.forClass(UpdateRecurringEventSeriesCommand.class);
        verify(series).update(stored.capture());
        assertThat(stored.getValue().participantIds()).isEqualTo(resolved);
    }

    @Test
    void anEventCopiedOrMovedIntoAPersonalSpaceTakesItsOwnerAsParticipant() {
        // Participants are local to a space and dropped on the way; the destination's rule decides.
        UUID personalSpace = UUID.randomUUID();
        SpaceMembership there = new SpaceMembership(UUID.randomUUID(), personalSpace, owner.userId(), SpaceRole.OWNER, Instant.now());
        ResolveMembershipUseCase memberships = mock(ResolveMembershipUseCase.class);
        when(memberships.resolve(personalSpace, owner.userId())).thenReturn(there);
        when(rule.participantsFor(there, List.of())).thenReturn(resolved);
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId)));

        new CopyEventHandler(events, memberships, rule).copy(eventId, personalSpace, owner);
        new MoveEventHandler(events, memberships, mock(DeleteEventUseCase.class), rule).move(eventId, personalSpace, owner);

        ArgumentCaptor<CreateEventCommand> stored = ArgumentCaptor.forClass(CreateEventCommand.class);
        verify(events, org.mockito.Mockito.times(2)).create(stored.capture());
        assertThat(stored.getAllValues()).allSatisfy(command -> assertThat(command.participantIds()).isEqualTo(resolved));
    }

    @Test
    void joiningOrLeavingIsRefusedWhereTheRuleClosesIt() {
        UUID eventId = UUID.randomUUID();
        when(events.findById(eventId)).thenReturn(Optional.of(event(eventId)));
        doThrow(new CalendarException.ParticipantsFixedInPersonalSpace()).when(rule).ensureParticipationOpen(owner);

        assertThatThrownBy(() -> new JoinEventHandler(events, rule).join(eventId, owner))
            .isInstanceOf(CalendarException.ParticipantsFixedInPersonalSpace.class);
        assertThatThrownBy(() -> new LeaveEventHandler(events, rule).leave(eventId, owner))
            .isInstanceOf(CalendarException.ParticipantsFixedInPersonalSpace.class);
        verify(events, never()).addParticipant(any(), any());
        verify(events, never()).removeParticipant(eq(eventId), any());
    }

    private CalendarEvent event(UUID id) {
        return new CalendarEvent(id, spaceId, "Dentiste", null, null, true, day, null, day, null, null,
            takingPart, null, null, owner.userId(), Instant.now());
    }

    private RecurringEventSeries weekly() {
        return new RecurringEventSeries(UUID.randomUUID(), spaceId, "Piano", null, null, true, null, null, 0, null,
            RecurrenceInterval.WEEKLY, 1, day, null, takingPart, owner.userId(), Instant.now());
    }
}
