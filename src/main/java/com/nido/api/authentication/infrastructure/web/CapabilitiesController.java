package com.nido.api.authentication.infrastructure.web;

import com.nido.api.authentication.application.dto.AuthCapabilities;
import com.nido.api.authentication.application.port.in.GetAuthCapabilitiesUseCase;
import com.nido.api.authentication.infrastructure.web.dto.CapabilitiesResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tells the sign-in pages what this installation offers. Public, and asked at runtime rather than
 * baked into the frontend build: the same image runs with and without an SMTP server.
 */
@Tag(name = "Authentication")
@RestController
public class CapabilitiesController {

    private final GetAuthCapabilitiesUseCase capabilities;

    public CapabilitiesController(GetAuthCapabilitiesUseCase capabilities) {
        this.capabilities = capabilities;
    }

    @Operation(
        summary = "Fonctionnalités de connexion disponibles",
        description = "Indique si le mot de passe oublié est disponible et si l'envoi de mails est configuré. Rate limit : 30 req/fenêtre."
    )
    @GetMapping("/api/auth/capabilities")
    @RateLimiting(max = 30)
    public ResponseEntity<CapabilitiesResponse> capabilities() {
        AuthCapabilities found = capabilities.capabilities();
        return ResponseEntity.ok(new CapabilitiesResponse(found.passwordReset(), found.mail()));
    }
}
