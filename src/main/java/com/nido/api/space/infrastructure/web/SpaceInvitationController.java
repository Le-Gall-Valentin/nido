package com.nido.api.space.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.application.port.in.InviteMemberUseCase;
import com.nido.api.space.application.port.in.ListSpaceInvitationsUseCase;
import com.nido.api.space.application.port.in.RevokeInvitationUseCase;
import com.nido.api.space.domain.model.InviteMemberCommand;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.infrastructure.web.dto.InviteMemberRequest;
import com.nido.api.space.infrastructure.web.dto.SpaceInvitationResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/invitations")
@Validated
@Tag(name = "Invitations", description = "Émission, liste et révocation des invitations d'un contexte")
public class SpaceInvitationController {

    private final InviteMemberUseCase inviteMemberUseCase;
    private final ListSpaceInvitationsUseCase listSpaceInvitationsUseCase;
    private final RevokeInvitationUseCase revokeInvitationUseCase;

    public SpaceInvitationController(InviteMemberUseCase inviteMemberUseCase,
                                     ListSpaceInvitationsUseCase listSpaceInvitationsUseCase,
                                     RevokeInvitationUseCase revokeInvitationUseCase) {
        this.inviteMemberUseCase = inviteMemberUseCase;
        this.listSpaceInvitationsUseCase = listSpaceInvitationsUseCase;
        this.revokeInvitationUseCase = revokeInvitationUseCase;
    }

    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpaceInvitationResponse> invite(
            @PathVariable UUID spaceId,
            @Valid @RequestBody InviteMemberRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        SpaceInvitationView view = inviteMemberUseCase.invite(
            new InviteMemberCommand(spaceId, request.email(), request.role(), membership.userId()), membership);
        SpaceInvitationResponse body = SpaceInvitationResponse.from(view);
        return ResponseEntity.created(URI.create("/api/spaces/" + spaceId + "/invitations/" + view.id())).body(body);
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SpaceInvitationResponse>> list(
            @PathVariable UUID spaceId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listSpaceInvitationsUseCase.list(membership).stream()
            .map(SpaceInvitationResponse::from)
            .toList());
    }

    @DeleteMapping("/{invitationId}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID spaceId,
            @PathVariable UUID invitationId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        revokeInvitationUseCase.revoke(spaceId, invitationId, membership);
        return ResponseEntity.noContent().build();
    }
}
