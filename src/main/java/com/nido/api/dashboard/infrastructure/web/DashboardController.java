package com.nido.api.dashboard.infrastructure.web;

import com.nido.api.dashboard.application.port.in.GetDashboardUseCase;
import com.nido.api.dashboard.infrastructure.web.dto.DashboardResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/dashboard")
@Validated
@Tag(name = "Tableau de bord", description = "Ce qui demande l'attention aujourd'hui dans un contexte")
public class DashboardController {

    private final GetDashboardUseCase getDashboardUseCase;

    public DashboardController(GetDashboardUseCase getDashboardUseCase) {
        this.getDashboardUseCase = getDashboardUseCase;
    }

    /** One request for the whole page — the client never calls the modules one by one. */
    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardResponse> get(
            @PathVariable UUID spaceId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        return ResponseEntity.ok(DashboardResponse.from(getDashboardUseCase.get(membership, caller.email())));
    }
}
