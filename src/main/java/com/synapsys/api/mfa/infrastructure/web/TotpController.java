package com.synapsys.api.mfa.infrastructure.web;

import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import com.synapsys.api.shared.security.AuthenticatedUser;
import com.synapsys.api.shared.security.CurrentUser;
import com.synapsys.api.mfa.application.port.in.ConfirmTotpUseCase;
import com.synapsys.api.mfa.application.port.in.DisableTotpUseCase;
import com.synapsys.api.mfa.application.port.in.GetTotpStatusUseCase;
import com.synapsys.api.mfa.application.port.in.SetupTotpUseCase;
import com.synapsys.api.mfa.application.dto.ConfirmTotpCommand;
import com.synapsys.api.mfa.application.dto.DisableTotpCommand;
import com.synapsys.api.mfa.application.dto.SetupTotpCommand;
import com.synapsys.api.mfa.domain.model.TotpSetupResult;
import com.synapsys.api.mfa.infrastructure.web.dto.TotpCodeRequest;
import com.synapsys.api.mfa.infrastructure.web.dto.TotpSetupResponse;
import com.synapsys.api.mfa.infrastructure.web.dto.TotpStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2FA - Management")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping("/api/auth/2fa")
public class TotpController {

    private final SetupTotpUseCase setupUseCase;
    private final ConfirmTotpUseCase confirmUseCase;
    private final DisableTotpUseCase disableUseCase;
    private final GetTotpStatusUseCase statusUseCase;

    public TotpController(SetupTotpUseCase setupUseCase,
                          ConfirmTotpUseCase confirmUseCase,
                          DisableTotpUseCase disableUseCase,
                          GetTotpStatusUseCase statusUseCase) {
        this.setupUseCase = setupUseCase;
        this.confirmUseCase = confirmUseCase;
        this.disableUseCase = disableUseCase;
        this.statusUseCase = statusUseCase;
    }

    @Operation(
        summary = "Initialiser la configuration TOTP",
        description = """
            Génère un secret TOTP et retourne l'URI `otpauth://` à scanner avec une application
            d'authentification (Google Authenticator, Authy, etc.).

            **La configuration n'est pas encore active** — l'utilisateur doit ensuite appeler
            `POST /api/auth/2fa/confirm` avec un code valide pour activer le TOTP.

            Si un setup est déjà en cours (non confirmé), un nouveau secret est généré
            et remplace l'ancien. Si le TOTP est déjà activé et confirmé, retourne `409`.

            Rate limit : 5 requêtes par fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Secret TOTP généré — scanner le QR code puis confirmer avec `POST /api/auth/2fa/confirm`",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TotpSetupResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Le TOTP est déjà activé et confirmé pour cet utilisateur",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/setup")
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpSetupResponse> setup(@Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        TotpSetupResult result = setupUseCase.setup(new SetupTotpCommand(caller.userId(), caller.email()));
        return ResponseEntity.ok(new TotpSetupResponse(result.otpauthUri(), result.secret()));
    }

    @Operation(
        summary = "Confirmer et activer le TOTP",
        description = """
            Valide le premier code TOTP après un setup et active définitivement l'authentification
            à deux facteurs sur le compte.

            Doit être appelé après `POST /api/auth/2fa/setup`. Si aucun setup n'est en cours,
            retourne `422`. Après un nombre excessif de tentatives incorrectes, le setup est
            annulé et retourne `429` — l'utilisateur doit relancer le setup.

            Rate limit : 10 requêtes par fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "TOTP activé avec succès", content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Format de code invalide (doit être 6 chiffres)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié ou code TOTP incorrect",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Aucun setup TOTP en cours — appeler d'abord `POST /api/auth/2fa/setup`",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de tentatives échouées — setup annulé, relancer depuis `POST /api/auth/2fa/setup`",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/confirm")
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirm(@Valid @RequestBody TotpCodeRequest request,
                                        @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        confirmUseCase.confirm(new ConfirmTotpCommand(caller.userId(), request.code()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Désactiver le TOTP",
        description = """
            Désactive l'authentification à deux facteurs sur le compte de l'utilisateur connecté.
            Un code TOTP valide est requis dans le corps pour confirmer l'intention.

            Si le TOTP n'est pas activé, retourne `409`.

            Rate limit : 5 requêtes par fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "TOTP désactivé", content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Format de code invalide (doit être 6 chiffres)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié ou code TOTP incorrect",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Le TOTP n'est pas activé sur ce compte",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @DeleteMapping
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disable(@Valid @RequestBody TotpCodeRequest request,
                                        @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        disableUseCase.disable(new DisableTotpCommand(caller.userId(), request.code()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Statut du TOTP",
        description = """
            Retourne si le TOTP est actuellement activé et confirmé pour l'utilisateur connecté.

            Rate limit : 30 requêtes par fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Statut récupéré",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TotpStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping("/status")
    @RateLimiting(max = 30)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpStatusResponse> status(@Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        boolean enabled = statusUseCase.isTotpEnabled(caller.userId());
        return ResponseEntity.ok(new TotpStatusResponse(enabled));
    }
}