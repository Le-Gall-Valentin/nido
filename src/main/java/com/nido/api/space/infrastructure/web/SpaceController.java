package com.nido.api.space.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import com.nido.api.space.application.port.in.CreateSharedSpaceUseCase;
import com.nido.api.space.application.port.in.DeleteSpaceUseCase;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.application.port.in.ListMySpacesUseCase;
import com.nido.api.space.application.port.in.UpdateSpaceUseCase;
import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.UpdateSpaceCommand;
import com.nido.api.space.infrastructure.web.dto.CreateSpaceRequest;
import com.nido.api.space.infrastructure.web.dto.SpaceDetailResponse;
import com.nido.api.space.infrastructure.web.dto.SpaceSummaryResponse;
import com.nido.api.space.infrastructure.web.dto.UpdateSpaceRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces")
@Validated
@Tag(name = "Contextes", description = "Espace perso et groupes partagés")
public class SpaceController {

    private final ListMySpacesUseCase listMySpacesUseCase;
    private final GetSpaceUseCase getSpaceUseCase;
    private final CreateSharedSpaceUseCase createSharedSpaceUseCase;
    private final UpdateSpaceUseCase updateSpaceUseCase;
    private final DeleteSpaceUseCase deleteSpaceUseCase;

    public SpaceController(ListMySpacesUseCase listMySpacesUseCase, GetSpaceUseCase getSpaceUseCase,
            CreateSharedSpaceUseCase createSharedSpaceUseCase, UpdateSpaceUseCase updateSpaceUseCase,
            DeleteSpaceUseCase deleteSpaceUseCase) {
        this.listMySpacesUseCase = listMySpacesUseCase;
        this.getSpaceUseCase = getSpaceUseCase;
        this.createSharedSpaceUseCase = createSharedSpaceUseCase;
        this.updateSpaceUseCase = updateSpaceUseCase;
        this.deleteSpaceUseCase = deleteSpaceUseCase;
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
        return ResponseEntity.ok(SpaceDetailResponse.from(getSpaceUseCase.get(spaceId, membership)));
    }

    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpaceDetailResponse> create(
            @Valid @RequestBody CreateSpaceRequest request,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        Space space = createSharedSpaceUseCase.create(new CreateSharedSpaceCommand(
            request.name(), request.description(), request.accent(), request.glyph(), caller.userId()));
        SpaceDetailResponse body = new SpaceDetailResponse(space.id(), space.type(), space.name(),
            space.description(), space.accent(), space.glyph(), SpaceRole.OWNER, 1);
        return ResponseEntity.created(URI.create("/api/spaces/" + space.id())).body(body);
    }

    @PatchMapping("/{spaceId}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> update(
            @PathVariable UUID spaceId,
            @Valid @RequestBody UpdateSpaceRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        updateSpaceUseCase.update(new UpdateSpaceCommand(
            spaceId, request.name(), request.description(), request.accent(), request.glyph()), membership);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{spaceId}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteSpaceUseCase.delete(membership);
        return ResponseEntity.noContent().build();
    }
}
