package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.application.port.in.CopyEventUseCase;
import com.nido.api.calendar.application.port.in.CreateEventUseCase;
import com.nido.api.calendar.application.port.in.DeleteEventUseCase;
import com.nido.api.calendar.application.port.in.JoinEventUseCase;
import com.nido.api.calendar.application.port.in.LeaveEventUseCase;
import com.nido.api.calendar.application.port.in.MoveEventUseCase;
import com.nido.api.calendar.application.port.in.UpdateEventUseCase;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.infrastructure.web.dto.CreateEventRequest;
import com.nido.api.calendar.infrastructure.web.dto.EventResponse;
import com.nido.api.calendar.infrastructure.web.dto.TransferEventRequest;
import com.nido.api.calendar.infrastructure.web.dto.UpdateEventRequest;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/calendar/events")
@Validated
@Tag(name = "Calendrier", description = "Événements du calendrier d'un contexte")
public class CalendarEventController {

    private final CreateEventUseCase createEventUseCase;
    private final UpdateEventUseCase updateEventUseCase;
    private final DeleteEventUseCase deleteEventUseCase;
    private final JoinEventUseCase joinEventUseCase;
    private final LeaveEventUseCase leaveEventUseCase;
    private final CopyEventUseCase copyEventUseCase;
    private final MoveEventUseCase moveEventUseCase;

    public CalendarEventController(CreateEventUseCase createEventUseCase,
                                   UpdateEventUseCase updateEventUseCase,
                                   DeleteEventUseCase deleteEventUseCase,
                                   JoinEventUseCase joinEventUseCase,
                                   LeaveEventUseCase leaveEventUseCase,
                                   CopyEventUseCase copyEventUseCase,
                                   MoveEventUseCase moveEventUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.updateEventUseCase = updateEventUseCase;
        this.deleteEventUseCase = deleteEventUseCase;
        this.joinEventUseCase = joinEventUseCase;
        this.leaveEventUseCase = leaveEventUseCase;
        this.copyEventUseCase = copyEventUseCase;
        this.moveEventUseCase = moveEventUseCase;
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody CreateEventRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(
            createEventUseCase.create(new CreateEventCommand(
                spaceId, request.title(), request.description(), request.location(), request.allDayOrDefault(),
                request.startDate(), request.startTime(), request.endDate(), request.endTime(),
                request.color(), request.participantIds(), null, null, membership.userId()), membership)));
    }

    @PatchMapping("/{eventId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID eventId, @Valid @RequestBody UpdateEventRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.ok(EventResponse.from(
            updateEventUseCase.update(new UpdateEventCommand(
                eventId, request.title(), request.description(), request.location(), request.allDayOrDefault(),
                request.startDate(), request.startTime(), request.endDate(), request.endTime(),
                request.color(), request.participantIds()), membership)));
    }

    @DeleteMapping("/{eventId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID eventId,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        deleteEventUseCase.delete(eventId, membership);
        return ResponseEntity.noContent().build();
    }

    // A convenience over the full update, not a separate permission: a MEMBER can already rewrite
    // the participant list. It lets the UI offer "Me joindre" / "Me retirer" without a form.
    @PostMapping("/{eventId}/participants/me")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> join(
            @PathVariable UUID spaceId, @PathVariable UUID eventId,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        joinEventUseCase.join(eventId, membership);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{eventId}/participants/me")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> leave(
            @PathVariable UUID spaceId, @PathVariable UUID eventId,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        leaveEventUseCase.leave(eventId, membership);
        return ResponseEntity.noContent().build();
    }

    // Copying takes the default VIEWER floor: it writes into the DESTINATION space, not into the
    // route's spaceId, which it only reads — CopyEventHandler checks write access on the
    // destination membership instead. Moving declares MEMBER, because it also deletes from the
    // source and so needs write access here too. Same split as RecipeController.
    
    @PostMapping("/{eventId}/copy")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> copy(
            @PathVariable UUID spaceId, @PathVariable UUID eventId,
            @Valid @RequestBody TransferEventRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(
            copyEventUseCase.copy(eventId, request.destinationSpaceId(), membership)));
    }

    @PostMapping("/{eventId}/move")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> move(
            @PathVariable UUID spaceId, @PathVariable UUID eventId,
            @Valid @RequestBody TransferEventRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(
            moveEventUseCase.move(eventId, request.destinationSpaceId(), membership)));
    }
}
