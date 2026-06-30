package com.synapsys.api.authentication.infrastructure.web;

import com.synapsys.api.authentication.application.dto.VerifyTotpChallengeCommand;
import com.synapsys.api.authentication.application.port.in.VerifyTotpChallengeUseCase;
import com.synapsys.api.authentication.domain.model.AuthenticationException;
import com.synapsys.api.authentication.domain.model.LoginResult;
import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.infrastructure.security.CookieService;
import com.synapsys.api.authentication.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2FA - Challenge")
@RestController
@RequestMapping("/api/auth/2fa")
public class TotpChallengeController {

    private final VerifyTotpChallengeUseCase verifyUseCase;
    private final CookieService cookieService;

    public TotpChallengeController(VerifyTotpChallengeUseCase verifyUseCase,
                                   CookieService cookieService) {
        this.verifyUseCase = verifyUseCase;
        this.cookieService = cookieService;
    }

    @Operation(
        summary = "Vérification du code TOTP lors du login",
        description = """
            Deuxième étape du login à deux facteurs. À appeler après un `POST /api/auth/login`
            qui a retourné `{ "totpRequired": true }`.

            Le cookie `totp_challenge` (posé lors du login) est obligatoire et sert d'identifiant
            de session temporaire. Ce cookie est effacé après validation.

            En cas de succès, les cookies `access_token` et `refresh_token` sont posés
            et l'utilisateur est pleinement authentifié.

            Rate limit : 5 requêtes par fenêtre.
            Après un trop grand nombre de tentatives TOTP incorrectes : `429` sans error_code.
            Si le cookie `totp_challenge` est absent ou expiré : `401` avec `error_code: totp_challenge_expired`.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Code TOTP valide — utilisateur authentifié, cookies `access_token` et `refresh_token` posés",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserInfoResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Code invalide (format incorrect — doit être exactement 6 chiffres)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Cookie `totp_challenge` absent ou expiré (`error_code: totp_challenge_expired`), ou code TOTP incorrect",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de tentatives TOTP échouées ou rate limit global dépassé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/verify")
    @RateLimiting(max = 5)
    public ResponseEntity<UserInfoResponse> verify(@Valid @RequestBody VerifyRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse response) {
        String challengeId = cookieService
            .extractFromRequest(httpRequest, CookieService.TOTP_CHALLENGE_COOKIE)
            .orElseThrow(AuthenticationException.TotpChallengeExpired::new);

        LoginResult.Success result = verifyUseCase.verify(
            new VerifyTotpChallengeCommand(challengeId, request.code())
        );

        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildAccessCookie(result.tokens().accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildRefreshCookie(result.tokens().refreshToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildClearChallengeCookie().toString());

        UserCredentials user = result.credentials();
        return ResponseEntity.ok(new UserInfoResponse(user.id(), user.username(), user.email(), user.role(), user.createdAt(), true));
    }

    @Schema(description = "Code TOTP à 6 chiffres pour valider le challenge de connexion")
    record VerifyRequest(
        @Schema(description = "Code TOTP (exactement 6 chiffres)", example = "123456", pattern = "\\d{6}")
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit number") String code
    ) {}
}