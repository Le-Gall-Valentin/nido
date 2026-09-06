package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.application.port.in.GetFinanceStatsUseCase;
import com.nido.api.finance.application.port.in.GetProjectionUseCase;
import com.nido.api.finance.infrastructure.web.dto.FinanceStatsResponse;
import com.nido.api.finance.infrastructure.web.dto.ProjectionResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/finance")
@Validated
@Tag(name = "Finances", description = "Statistiques et projection d'un contexte")
public class FinanceStatsController {

    private final GetFinanceStatsUseCase getFinanceStatsUseCase;
    private final GetProjectionUseCase getProjectionUseCase;

    public FinanceStatsController(GetFinanceStatsUseCase getFinanceStatsUseCase, GetProjectionUseCase getProjectionUseCase) {
        this.getFinanceStatsUseCase = getFinanceStatsUseCase;
        this.getProjectionUseCase = getProjectionUseCase;
    }

    @GetMapping("/stats")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinanceStatsResponse> stats(
            @PathVariable UUID spaceId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(FinanceStatsResponse.from(getFinanceStatsUseCase.getStats(month, membership)));
    }

    @GetMapping("/projection")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectionResponse> projection(
            @PathVariable UUID spaceId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(ProjectionResponse.from(getProjectionUseCase.getProjection(month, membership)));
    }
}
