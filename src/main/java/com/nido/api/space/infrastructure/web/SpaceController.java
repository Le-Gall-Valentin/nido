package com.nido.api.space.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import com.nido.api.space.application.port.in.ListMySpacesUseCase;
import com.nido.api.space.infrastructure.web.dto.SpaceSummaryResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spaces")
@Validated
@Tag(name = "Contextes", description = "Espace perso et groupes partagés")
public class SpaceController {

    private final ListMySpacesUseCase listMySpacesUseCase;

    public SpaceController(ListMySpacesUseCase listMySpacesUseCase) {
        this.listMySpacesUseCase = listMySpacesUseCase;
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
}
