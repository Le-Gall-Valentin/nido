package com.nido.api.space.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.application.port.in.ChangeMemberRoleUseCase;
import com.nido.api.space.application.port.in.ListSpaceMembersUseCase;
import com.nido.api.space.application.port.in.RemoveMemberUseCase;
import com.nido.api.space.application.port.in.TransferOwnershipUseCase;
import com.nido.api.space.domain.model.ChangeMemberRoleCommand;
import com.nido.api.space.domain.model.RemoveMemberCommand;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.TransferOwnershipCommand;
import com.nido.api.space.infrastructure.web.dto.ChangeMemberRoleRequest;
import com.nido.api.space.infrastructure.web.dto.SpaceMemberResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/members")
@Validated
@Tag(name = "Membres d'un contexte", description = "Adhésions et rôles à l'intérieur d'un contexte")
public class SpaceMemberController {

    private final ListSpaceMembersUseCase listSpaceMembersUseCase;
    private final ChangeMemberRoleUseCase changeMemberRoleUseCase;
    private final RemoveMemberUseCase removeMemberUseCase;
    private final TransferOwnershipUseCase transferOwnershipUseCase;

    public SpaceMemberController(ListSpaceMembersUseCase listSpaceMembersUseCase,
                                 ChangeMemberRoleUseCase changeMemberRoleUseCase,
                                 RemoveMemberUseCase removeMemberUseCase,
                                 TransferOwnershipUseCase transferOwnershipUseCase) {
        this.listSpaceMembersUseCase = listSpaceMembersUseCase;
        this.changeMemberRoleUseCase = changeMemberRoleUseCase;
        this.removeMemberUseCase = removeMemberUseCase;
        this.transferOwnershipUseCase = transferOwnershipUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SpaceMemberResponse>> listMembers(
            @PathVariable UUID spaceId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listSpaceMembersUseCase.list(membership).stream()
            .map(SpaceMemberResponse::from)
            .toList());
    }

    @PatchMapping("/{userId}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changeMemberRole(
            @PathVariable UUID spaceId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeMemberRoleRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        changeMemberRoleUseCase.change(new ChangeMemberRoleCommand(spaceId, userId, request.role()), membership);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID spaceId,
            @PathVariable UUID userId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        removeMemberUseCase.remove(new RemoveMemberCommand(spaceId, userId), membership);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/ownership")
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable UUID spaceId,
            @PathVariable UUID userId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        transferOwnershipUseCase.transfer(new TransferOwnershipCommand(spaceId, userId), membership);
        return ResponseEntity.noContent().build();
    }
}
