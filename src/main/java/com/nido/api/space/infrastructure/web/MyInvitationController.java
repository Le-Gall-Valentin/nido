package com.nido.api.space.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimitMode;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import com.nido.api.space.application.port.in.AcceptInvitationUseCase;
import com.nido.api.space.application.port.in.ListMyInvitationsUseCase;
import com.nido.api.space.domain.model.AcceptInvitationCommand;
import com.nido.api.space.infrastructure.web.dto.AcceptInvitationRequest;
import com.nido.api.space.infrastructure.web.dto.ReceivedInvitationResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Deux routes qui ne sont pas des routes de contexte : ni {@code {spaceId}}, ni
 * {@code @CurrentMembership}. L'appelant est identifié par {@code @CurrentUser}, dont on
 * lit l'adresse email — celle qui reçoit les invitations.
 */
@RestController
@Validated
@Tag(name = "Invitations reçues", description = "Invitations adressées à l'utilisateur connecté")
public class MyInvitationController {

    private final ListMyInvitationsUseCase listMyInvitationsUseCase;
    private final AcceptInvitationUseCase acceptInvitationUseCase;

    public MyInvitationController(ListMyInvitationsUseCase listMyInvitationsUseCase,
                                  AcceptInvitationUseCase acceptInvitationUseCase) {
        this.listMyInvitationsUseCase = listMyInvitationsUseCase;
        this.acceptInvitationUseCase = acceptInvitationUseCase;
    }

    @GetMapping("/api/me/invitations")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReceivedInvitationResponse>> listMine(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        List<ReceivedInvitationResponse> body = listMyInvitationsUseCase.listMine(caller.email()).stream()
            .map(ReceivedInvitationResponse::from)
            .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/api/invitations/accept")
    @RateLimiting(max = 20)
    @RateLimiting(mode = RateLimitMode.USER, max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AcceptInvitationResponse> accept(
            @Valid @RequestBody AcceptInvitationRequest request,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        UUID spaceId = acceptInvitationUseCase.accept(
            new AcceptInvitationCommand(request.code()), caller.userId(), caller.email());
        return ResponseEntity.ok(new AcceptInvitationResponse(spaceId));
    }

    private record AcceptInvitationResponse(UUID spaceId) {}
}
