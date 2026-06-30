package com.synapsys.api.authentication.infrastructure.web;

import com.synapsys.api.authentication.application.dto.LoginCommand;
import com.synapsys.api.authentication.application.port.in.LoginUseCase;
import com.synapsys.api.authentication.application.port.in.LogoutUseCase;
import com.synapsys.api.authentication.application.port.in.RefreshTokenUseCase;
import com.synapsys.api.authentication.domain.model.*;
import com.synapsys.api.authentication.infrastructure.security.CookieService;
import com.synapsys.api.authentication.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.authentication.infrastructure.web.dto.TotpRequiredResponse;
import com.synapsys.api.authentication.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final CookieService cookieService;

    public AuthController(LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          CookieService cookieService) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.cookieService = cookieService;
    }

    @Operation(
        summary = "Connexion utilisateur",
        description = """
            Authentifie un utilisateur avec ses identifiants.

            **Cas 1 — Succès sans 2FA** : retourne `200` avec les informations de l'utilisateur.
            Deux cookies HttpOnly sont posés : `access_token` et `refresh_token`.

            **Cas 2 — 2FA requis** : retourne `200` avec `{ "totpRequired": true }`.
            Un cookie `totp_challenge` est posé. Le client doit ensuite appeler
            `POST /api/auth/2fa/verify` avec le code TOTP pour finaliser la connexion.

            Rate limit : 5 requêtes par fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Authentification réussie (sans 2FA) ou challenge TOTP initié",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(oneOf = {UserInfoResponse.class, TotpRequiredResponse.class})
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Corps de requête invalide (champs manquants ou dépassement de taille)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Identifiants incorrects ou compte désactivé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de tentatives de connexion. Respecter le header `Retry-After`.",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/login")
    @RateLimiting(max = 5)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletResponse response) {
        LoginResult result = loginUseCase.login(new LoginCommand(request.username(), request.password()));
        return switch (result) {
            case LoginResult.Success s -> {
                response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildAccessCookie(s.tokens().accessToken()).toString());
                response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildRefreshCookie(s.tokens().refreshToken()).toString());
                UserCredentials user = s.credentials();
                yield ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.email(), user.role(), user.createdAt(), false));
            }
            case LoginResult.TotpRequired t -> {
                response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildChallengeCookie(t.challengeId()).toString());
                yield ResponseEntity.ok(new TotpRequiredResponse());
            }
        };
    }

    @Operation(
        summary = "Renouvellement du token d'accès",
        description = """
            Échange le cookie `refresh_token` contre une nouvelle paire de tokens.
            Les deux cookies (`access_token` et `refresh_token`) sont régénérés et posés en réponse.

            À appeler lorsque l'`access_token` expire (typiquement sur une erreur 401
            qui n'est pas `InvalidCredentials`).

            Rate limit : 5 requêtes par fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tokens renouvelés — nouveaux cookies `access_token` et `refresh_token` posés", content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Cookie `refresh_token` absent, expiré ou révoqué",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @SecurityRequirement(name = "refreshCookieAuth")
    @PostMapping("/refresh")
    @RateLimiting(max = 5)
    public ResponseEntity<Void> refresh(HttpServletRequest request,
                                        HttpServletResponse response) {
        String rawRefreshToken = cookieService
            .extractFromRequest(request, CookieService.REFRESH_COOKIE)
            .orElseThrow(AuthenticationException.TokenNotFound::new);
        var tokens = refreshTokenUseCase.refresh(rawRefreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildAccessCookie(tokens.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildRefreshCookie(tokens.refreshToken()).toString());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Déconnexion",
        description = """
            Révoque le `refresh_token` côté serveur et efface les cookies `access_token`
            et `refresh_token`. Le cookie `totp_challenge`, s'il est présent, n'est pas
            effacé par cet appel — il expire naturellement après sa TTL.

            Si le cookie `refresh_token` est absent, la déconnexion reste effective
            (les cookies sont quand même effacés) — aucune erreur n'est levée.

            Rate limit : 10 requêtes par fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Déconnexion réussie — cookies effacés", content = @Content),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/logout")
    @RateLimiting(max = 10)
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        String rawRefreshToken = cookieService
            .extractFromRequest(request, CookieService.REFRESH_COOKIE)
            .orElse(null);
        logoutUseCase.logout(rawRefreshToken);
        cookieService.buildClearCookies().forEach(c -> response.addHeader(HttpHeaders.SET_COOKIE, c.toString()));
        return ResponseEntity.noContent().build();
    }
}