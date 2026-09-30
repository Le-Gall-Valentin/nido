package com.nido.api.authentication.infrastructure.web;

import com.nido.api.authentication.application.port.in.CheckPasswordResetTokenUseCase;
import com.nido.api.authentication.application.port.in.ConfirmPasswordResetUseCase;
import com.nido.api.authentication.application.port.in.RequestPasswordResetUseCase;
import com.nido.api.authentication.infrastructure.web.dto.ConfirmPasswordResetRequest;
import com.nido.api.authentication.infrastructure.web.dto.PasswordResetTokenRequest;
import com.nido.api.authentication.infrastructure.web.dto.RequestPasswordResetRequest;
import com.nido.api.infrastructure.config.ConditionalOnMailEnabled;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Forgot password". Registered only when mail is on: without a way to send the link the routes
 * would promise something the installation cannot do, so they do not exist — a request falls through
 * to the static-resource handler, which refuses it (405 for these POSTs, 404 elsewhere).
 *
 * <p>The token always travels in a request body — never in a URL — so it appears in no access log.
 */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth/password-reset")
@ConditionalOnMailEnabled
public class PasswordResetController {

    private final RequestPasswordResetUseCase requestReset;
    private final CheckPasswordResetTokenUseCase checkToken;
    private final ConfirmPasswordResetUseCase confirmReset;

    public PasswordResetController(RequestPasswordResetUseCase requestReset,
                                   CheckPasswordResetTokenUseCase checkToken,
                                   ConfirmPasswordResetUseCase confirmReset) {
        this.requestReset = requestReset;
        this.checkToken = checkToken;
        this.confirmReset = confirmReset;
    }

    @Operation(
        summary = "Demander un lien de réinitialisation",
        description = """
            Envoie un lien valable 30 minutes au titulaire du compte désigné par son identifiant ou son adresse,
            dans la langue du compte. La réponse est **toujours 202, sans corps** — compte existant ou non,
            désactivé ou non — pour ne rien révéler. Un seul lien par compte toutes les 5 minutes.

            Rate limit : 5 requêtes par 15 minutes.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Demande prise en compte", content = @Content),
        @ApiResponse(responseCode = "400", description = "Identifiant absent ou trop long",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de demandes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/request")
    @RateLimiting(max = 5, windowSeconds = 900)
    public ResponseEntity<Void> request(@Valid @RequestBody RequestPasswordResetRequest body) {
        requestReset.request(body.identifier());
        return ResponseEntity.accepted().build();
    }

    @Operation(
        summary = "Vérifier un lien de réinitialisation",
        description = "204 si le lien est utilisable ; 410 s'il a expiré, a déjà servi, a été remplacé ou n'existe pas. Rate limit : 10 req/fenêtre."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Lien utilisable", content = @Content),
        @ApiResponse(responseCode = "400", description = "Jeton absent ou trop long",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "410", description = "Lien plus valable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de demandes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/check")
    @RateLimiting(max = 10)
    public ResponseEntity<Void> check(@Valid @RequestBody PasswordResetTokenRequest body) {
        checkToken.check(body.token());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Choisir un nouveau mot de passe",
        description = """
            Change le mot de passe, met fin à toutes les sessions, invalide tous les liens du compte et prévient
            le titulaire par mail. Ne connecte pas : la connexion suivante demande le nouveau mot de passe, et le
            code 2FA s'il est activé. Rate limit : 10 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mot de passe changé", content = @Content),
        @ApiResponse(responseCode = "400", description = "Mot de passe refusé par les règles, ou jeton absent ou trop long",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "410", description = "Lien plus valable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de demandes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/confirm")
    @RateLimiting(max = 10)
    public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmPasswordResetRequest body) {
        confirmReset.confirm(body.token(), body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
