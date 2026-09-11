package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.application.port.in.CreateRecurringSeriesUseCase;
import com.nido.api.finance.application.port.in.DeleteRecurringSeriesUseCase;
import com.nido.api.finance.application.port.in.ListRecurringSeriesUseCase;
import com.nido.api.finance.application.port.in.UpdateRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;
import com.nido.api.finance.infrastructure.web.dto.CreateRecurringSeriesRequest;
import com.nido.api.finance.infrastructure.web.dto.RecurringSeriesResponse;
import com.nido.api.finance.infrastructure.web.dto.UpdateRecurringSeriesRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.nido.api.finance.infrastructure.web.FinanceTransactionController.toContributionInputs;

@RestController
@RequestMapping("/api/spaces/{spaceId}/finance/recurring-series")
@Validated
@Tag(name = "Finances", description = "Opérations financières récurrentes d'un contexte")
public class RecurringTransactionSeriesController {

    private final ListRecurringSeriesUseCase listRecurringSeriesUseCase;
    private final CreateRecurringSeriesUseCase createRecurringSeriesUseCase;
    private final UpdateRecurringSeriesUseCase updateRecurringSeriesUseCase;
    private final DeleteRecurringSeriesUseCase deleteRecurringSeriesUseCase;

    public RecurringTransactionSeriesController(ListRecurringSeriesUseCase listRecurringSeriesUseCase,
                                                 CreateRecurringSeriesUseCase createRecurringSeriesUseCase,
                                                 UpdateRecurringSeriesUseCase updateRecurringSeriesUseCase,
                                                 DeleteRecurringSeriesUseCase deleteRecurringSeriesUseCase) {
        this.listRecurringSeriesUseCase = listRecurringSeriesUseCase;
        this.createRecurringSeriesUseCase = createRecurringSeriesUseCase;
        this.updateRecurringSeriesUseCase = updateRecurringSeriesUseCase;
        this.deleteRecurringSeriesUseCase = deleteRecurringSeriesUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecurringSeriesResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listRecurringSeriesUseCase.list(membership).stream().map(RecurringSeriesResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecurringSeriesResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody CreateRecurringSeriesRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        RecurringTransactionSeries created = createRecurringSeriesUseCase.create(new CreateRecurringSeriesCommand(
            spaceId, request.label(), request.amount(), request.type(), request.categoryId(), request.payerId(),
            toContributionInputs(request.contributors()), request.recurrence().intervalType(), request.recurrence().intervalCount(),
            request.recurrence().anchorDate(), request.recurrence().endDate()), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurringSeriesResponse.from(created));
    }

    @PatchMapping("/{seriesId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecurringSeriesResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID seriesId, @Valid @RequestBody UpdateRecurringSeriesRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        RecurringTransactionSeries updated = updateRecurringSeriesUseCase.update(new UpdateRecurringSeriesCommand(
            seriesId, spaceId, request.label(), request.amount(), request.type(), request.categoryId(), request.payerId(),
            toContributionInputs(request.contributors()), request.recurrence().intervalType(), request.recurrence().intervalCount(),
            request.recurrence().anchorDate(), request.recurrence().endDate()), membership);
        return ResponseEntity.ok(RecurringSeriesResponse.from(updated));
    }

    @DeleteMapping("/{seriesId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID seriesId,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        deleteRecurringSeriesUseCase.delete(seriesId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }
}
