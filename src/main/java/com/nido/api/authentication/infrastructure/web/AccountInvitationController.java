package com.nido.api.authentication.infrastructure.web;

import com.nido.api.authentication.application.port.in.AcceptAccountInvitationUseCase;
import com.nido.api.authentication.application.port.in.CheckAccountInvitationUseCase;
import com.nido.api.authentication.infrastructure.web.dto.AcceptAccountInvitationRequest;
import com.nido.api.authentication.infrastructure.web.dto.AccountInvitationCheckResponse;
import com.nido.api.authentication.infrastructure.web.dto.AccountInvitationTokenRequest;
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
 * The invitation link's routes, used by definition by someone who cannot sign in yet. They answer whether
 * mail is on or off: without mail the administrator passes the link on by other means.
 *
 * <p>The token always travels in a request body — never in a URL — so it appears in no access log.
 */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth/account-invitation")
public class AccountInvitationController {

    private final CheckAccountInvitationUseCase checkInvitation;
    private final AcceptAccountInvitationUseCase acceptInvitation;

    public AccountInvitationController(CheckAccountInvitationUseCase checkInvitation,
                                       AcceptAccountInvitationUseCase acceptInvitation) {
        this.checkInvitation = checkInvitation;
        this.acceptInvitation = acceptInvitation;
    }

    @Operation(
        summary = "Vérifier un lien d'invitation",
        description = "200 avec le nom d'utilisateur du compte invité si le lien est utilisable ; 410 s'il a expiré, a déjà servi, a été remplacé, n'existe pas ou si le compte est désactivé. Rate limit : 10 req/fenêtre."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lien utilisable",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountInvitationCheckResponse.class))),
        @ApiResponse(responseCode = "400", description = "Jeton absent ou trop long",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "410", description = "Lien plus valable (`error_code` : `invitation_link_invalid`)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de demandes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/check")
    @RateLimiting(max = 10)
    public ResponseEntity<AccountInvitationCheckResponse> check(@Valid @RequestBody AccountInvitationTokenRequest body) {
        return ResponseEntity.ok(new AccountInvitationCheckResponse(checkInvitation.check(body.token())));
    }

    @Operation(
        summary = "Choisir son premier mot de passe",
        description = """
            Enregistre le mot de passe du compte invité et met fin à l'invitation : le lien ne sert plus. Ne connecte
            pas : la connexion suivante demande ce mot de passe. Rate limit : 10 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mot de passe enregistré", content = @Content),
        @ApiResponse(responseCode = "400", description = "Mot de passe refusé par les règles, ou jeton absent ou trop long",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "410", description = "Lien plus valable (`error_code` : `invitation_link_invalid`)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de demandes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/accept")
    @RateLimiting(max = 10)
    public ResponseEntity<Void> accept(@Valid @RequestBody AcceptAccountInvitationRequest body) {
        acceptInvitation.accept(body.token(), body.password());
        return ResponseEntity.noContent().build();
    }
}
