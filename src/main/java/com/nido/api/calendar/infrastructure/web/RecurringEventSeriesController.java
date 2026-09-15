package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.application.port.in.CreateRecurringEventSeriesUseCase;
import com.nido.api.calendar.application.port.in.DeleteRecurringEventSeriesUseCase;
import com.nido.api.calendar.application.port.in.DetachOccurrenceUseCase;
import com.nido.api.calendar.application.port.in.ExcludeOccurrenceUseCase;
import com.nido.api.calendar.application.port.in.ListRecurringEventSeriesUseCase;
import com.nido.api.calendar.application.port.in.UpdateRecurringEventSeriesUseCase;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.infrastructure.web.dto.EventResponse;
import com.nido.api.calendar.infrastructure.web.dto.RecurringEventSeriesRequest;
import com.nido.api.calendar.infrastructure.web.dto.RecurringEventSeriesResponse;
import com.nido.api.calendar.infrastructure.web.dto.UpdateEventRequest;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/calendar/recurring-event-series")
@Validated
@Tag(name = "Calendrier", description = "Séries d'événements récurrents d'un contexte")
public class RecurringEventSeriesController {

    private final ListRecurringEventSeriesUseCase listUseCase;
    private final CreateRecurringEventSeriesUseCase createUseCase;
    private final UpdateRecurringEventSeriesUseCase updateUseCase;
    private final DeleteRecurringEventSeriesUseCase deleteUseCase;
    private final DetachOccurrenceUseCase detachUseCase;
    private final ExcludeOccurrenceUseCase excludeUseCase;

    public RecurringEventSeriesController(ListRecurringEventSeriesUseCase listUseCase,
                                          CreateRecurringEventSeriesUseCase createUseCase,
                                          UpdateRecurringEventSeriesUseCase updateUseCase,
                                          DeleteRecurringEventSeriesUseCase deleteUseCase,
                                          DetachOccurrenceUseCase detachUseCase,
                                          ExcludeOccurrenceUseCase excludeUseCase) {
        this.listUseCase = listUseCase;
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.detachUseCase = detachUseCase;
        this.excludeUseCase = excludeUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecurringEventSeriesResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listUseCase.list(membership).stream()
            .map(RecurringEventSeriesResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecurringEventSeriesResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody RecurringEventSeriesRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurringEventSeriesResponse.from(
            createUseCase.create(new CreateRecurringEventSeriesCommand(
                spaceId, request.title(), request.description(), request.location(), request.allDay(),
                request.startTime(), request.endTime(), request.durationDays(), request.color(),
                request.intervalType(), request.intervalCount(), request.anchorDate(), request.endDate(),
                request.participantIds(), membership.userId()), membership)));
    }

    @PatchMapping("/{seriesId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecurringEventSeriesResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID seriesId,
            @Valid @RequestBody RecurringEventSeriesRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.ok(RecurringEventSeriesResponse.from(
            updateUseCase.update(new UpdateRecurringEventSeriesCommand(
                seriesId, request.title(), request.description(), request.location(), request.allDay(),
                request.startTime(), request.endTime(), request.durationDays(), request.color(),
                request.intervalType(), request.intervalCount(), request.anchorDate(), request.endDate(),
                request.participantIds()), membership)));
    }

    @DeleteMapping("/{seriesId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID seriesId,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        deleteUseCase.delete(seriesId, membership);
        return ResponseEntity.noContent().build();
    }

    /**
     * The only PUT in the codebase, deliberately: this is an idempotent upsert of the
     * (series, slot) pair, not a partial update of an existing resource. Replaying it must give
     * the same state, which is what PATCH and POST do not promise.
     */
    @PutMapping("/{seriesId}/occurrences/{date}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> detachOccurrence(
            @PathVariable UUID spaceId, @PathVariable UUID seriesId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpdateEventRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.ok(EventResponse.from(detachUseCase.detach(seriesId, date, new UpdateEventCommand(
            null, request.title(), request.description(), request.location(), request.allDay(),
            request.startDate(), request.startTime(), request.endDate(), request.endTime(),
            request.color(), request.participantIds()), membership)));
    }

    @DeleteMapping("/{seriesId}/occurrences/{date}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> excludeOccurrence(
            @PathVariable UUID spaceId, @PathVariable UUID seriesId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        excludeUseCase.exclude(seriesId, date, membership);
        return ResponseEntity.noContent().build();
    }
}
