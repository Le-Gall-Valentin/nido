package com.nido.api.tasks.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.DeleteRecurringTaskSeriesUseCase;
import com.nido.api.tasks.application.port.in.ListRecurringTaskSeriesUseCase;
import com.nido.api.tasks.application.port.in.UpdateRecurringTaskSeriesUseCase;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;
import com.nido.api.tasks.infrastructure.web.dto.RecurringTaskSeriesResponse;
import com.nido.api.tasks.infrastructure.web.dto.UpdateRecurringTaskSeriesRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/recurring-task-series")
@Validated
@Tag(name = "Tâches", description = "Séries de tâches récurrentes d'un contexte")
public class RecurringTaskSeriesController {

    private final ListRecurringTaskSeriesUseCase listRecurringTaskSeriesUseCase;
    private final UpdateRecurringTaskSeriesUseCase updateRecurringTaskSeriesUseCase;
    private final DeleteRecurringTaskSeriesUseCase deleteRecurringTaskSeriesUseCase;

    public RecurringTaskSeriesController(ListRecurringTaskSeriesUseCase listRecurringTaskSeriesUseCase,
                                          UpdateRecurringTaskSeriesUseCase updateRecurringTaskSeriesUseCase,
                                          DeleteRecurringTaskSeriesUseCase deleteRecurringTaskSeriesUseCase) {
        this.listRecurringTaskSeriesUseCase = listRecurringTaskSeriesUseCase;
        this.updateRecurringTaskSeriesUseCase = updateRecurringTaskSeriesUseCase;
        this.deleteRecurringTaskSeriesUseCase = deleteRecurringTaskSeriesUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecurringTaskSeriesResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listRecurringTaskSeriesUseCase.list(membership).stream().map(RecurringTaskSeriesResponse::from).toList());
    }

    @PatchMapping("/{seriesId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecurringTaskSeriesResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID seriesId, @Valid @RequestBody UpdateRecurringTaskSeriesRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        RecurringTaskSeries updated = updateRecurringTaskSeriesUseCase.update(new UpdateRecurringTaskSeriesCommand(
            seriesId, spaceId, request.title(), request.priority(), request.subtasks(),
            request.recurrence().intervalType(), request.recurrence().intervalCount(),
            request.recurrence().leadIntervalType(), request.recurrence().leadIntervalCount(),
            request.recurrence().anchorDate(), request.recurrence().endDate(),
            request.recurrence().rotationMemberIds()), membership);
        return ResponseEntity.ok(RecurringTaskSeriesResponse.from(updated));
    }

    @DeleteMapping("/{seriesId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID seriesId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteRecurringTaskSeriesUseCase.delete(seriesId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }
}
