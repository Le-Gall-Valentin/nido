package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.application.port.in.ClearOpeningBalanceUseCase;
import com.nido.api.finance.application.port.in.GetOpeningBalanceUseCase;
import com.nido.api.finance.application.port.in.SetOpeningBalanceUseCase;
import com.nido.api.finance.domain.model.SetOpeningBalanceCommand;
import com.nido.api.finance.infrastructure.web.dto.OpeningBalanceRequest;
import com.nido.api.finance.infrastructure.web.dto.OpeningBalanceResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/finance/opening-balance")
@Validated
@Tag(name = "Finances", description = "Solde de départ d'un contexte")
public class FinanceOpeningBalanceController {

    private final GetOpeningBalanceUseCase getOpeningBalanceUseCase;
    private final SetOpeningBalanceUseCase setOpeningBalanceUseCase;
    private final ClearOpeningBalanceUseCase clearOpeningBalanceUseCase;

    public FinanceOpeningBalanceController(GetOpeningBalanceUseCase getOpeningBalanceUseCase,
                                           SetOpeningBalanceUseCase setOpeningBalanceUseCase,
                                           ClearOpeningBalanceUseCase clearOpeningBalanceUseCase) {
        this.getOpeningBalanceUseCase = getOpeningBalanceUseCase;
        this.setOpeningBalanceUseCase = setOpeningBalanceUseCase;
        this.clearOpeningBalanceUseCase = clearOpeningBalanceUseCase;
    }

    /** 204 when the space has none. */
    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OpeningBalanceResponse> get(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return getOpeningBalanceUseCase.get(membership)
            .map(openingBalance -> ResponseEntity.ok(OpeningBalanceResponse.from(openingBalance)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OpeningBalanceResponse> set(
            @PathVariable UUID spaceId, @Valid @RequestBody OpeningBalanceRequest request,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        return ResponseEntity.ok(OpeningBalanceResponse.from(setOpeningBalanceUseCase.set(
            new SetOpeningBalanceCommand(spaceId, request.amount(), request.asOf()), membership)));
    }

    @DeleteMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clear(
            @PathVariable UUID spaceId,
            @Parameter(hidden = true) @CurrentMembership(min = SpaceRole.MEMBER) SpaceMembership membership) {
        clearOpeningBalanceUseCase.clear(spaceId, membership);
        return ResponseEntity.noContent().build();
    }
}
