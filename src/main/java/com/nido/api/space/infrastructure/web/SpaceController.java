package com.nido.api.space.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.application.port.in.ListMySpacesUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.infrastructure.web.dto.SpaceDetailResponse;
import com.nido.api.space.infrastructure.web.dto.SpaceSummaryResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces")
@Validated
@Tag(name = "Contextes", description = "Espace perso et groupes partagés")
public class SpaceController {

    private final ListMySpacesUseCase listMySpacesUseCase;
    private final GetSpaceUseCase getSpaceUseCase;

    public SpaceController(ListMySpacesUseCase listMySpacesUseCase, GetSpaceUseCase getSpaceUseCase) {
        this.listMySpacesUseCase = listMySpacesUseCase;
        this.getSpaceUseCase = getSpaceUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SpaceSummaryResponse>> listMine(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        List<SpaceSummaryResponse> body = listMySpacesUseCase.listMine(caller.userId()).stream()
            .map(SpaceSummaryResponse::from)
            .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{spaceId}")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpaceDetailResponse> getSpace(
            @PathVariable UUID spaceId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(SpaceDetailResponse.from(getSpaceUseCase.get(membership)));
    }
}
