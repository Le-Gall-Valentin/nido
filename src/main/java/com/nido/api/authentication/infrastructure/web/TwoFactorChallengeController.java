package com.nido.api.authentication.infrastructure.web;

import com.nido.api.authentication.application.dto.VerifyTwoFactorChallengeCommand;
import com.nido.api.authentication.application.port.in.SendChallengeMailCodeUseCase;
import com.nido.api.authentication.application.port.in.VerifyTwoFactorChallengeUseCase;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.LoginResult;
import com.nido.api.authentication.infrastructure.security.CookieService;
import com.nido.api.authentication.infrastructure.web.dto.MailCodeResponse;
import com.nido.api.authentication.infrastructure.web.dto.UserInfoResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.shared.model.TwoFactorMethod;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2FA - Challenge")
@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorChallengeController {

    private final VerifyTwoFactorChallengeUseCase verifyUseCase;
    private final SendChallengeMailCodeUseCase sendMailCode;
    private final CookieService cookieService;

    public TwoFactorChallengeController(VerifyTwoFactorChallengeUseCase verifyUseCase,
                                        SendChallengeMailCodeUseCase sendMailCode,
                                        CookieService cookieService) {
        this.verifyUseCase = verifyUseCase;
        this.sendMailCode = sendMailCode;
        this.cookieService = cookieService;
    }

    @Operation(
        summary = "Vérification du second facteur lors du login",
        description = """
            Deuxième étape du login à deux facteurs. À appeler après un `POST /api/auth/login`
            qui a retourné `{ "twoFactorRequired": true }`, avec `{ "method": "APP" | "MAIL", "code" }`.

            Le cookie `two_factor_challenge` (posé lors du login) est obligatoire et sert d'identifiant
            de session temporaire. Ce cookie est effacé après validation.

            En cas de succès, les cookies `access_token` et `refresh_token` sont posés
            et l'utilisateur est pleinement authentifié.

            Rate limit : 5 requêtes par fenêtre.
            Après un trop grand nombre de codes incorrects, toutes méthodes confondues : `429` sans error_code.
            Méthode retirée ou en pause depuis le login : `409` `method_not_enabled` ou `method_unavailable`.
            Si le cookie `two_factor_challenge` est absent ou expiré : `401` avec
            `error_code: two_factor_challenge_expired`.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Code valide — utilisateur authentifié, cookies `access_token` et `refresh_token` posés",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserInfoResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Méthode absente ou code au mauvais format (exactement 6 chiffres)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Cookie `two_factor_challenge` absent ou expiré (`error_code: two_factor_challenge_expired`), ou code incorrect",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Méthode qui n'est pas activée (`method_not_enabled`) ou en pause (`method_unavailable`)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de codes incorrects ou rate limit global dépassé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/verify")
    @RateLimiting(max = 5)
    public ResponseEntity<UserInfoResponse> verify(@Valid @RequestBody VerifyRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse response) {
        String challengeId = challengeOf(httpRequest);
        LoginResult.Success result = verifyUseCase.verify(
            new VerifyTwoFactorChallengeCommand(challengeId, request.method(), request.code()));

        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildAccessCookie(result.tokens().accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildRefreshCookie(result.tokens().refreshToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildClearChallengeCookie().toString());
        return ResponseEntity.ok(UserInfoResponse.of(result.credentials(), result.twoFactorMethods()));
    }

    @Operation(summary = "Recevoir le code de connexion par mail", description = """
        Pendant une connexion en deux étapes (cookie `two_factor_challenge`), envoie à l'adresse du compte un code
        valable 10 minutes, lié à cette connexion. Sert au choix « Code par mail » et à « Renvoyer ».
        `200 { sent: true, resendAfterSeconds }` ; `429` `resend_too_soon` ou `send_limit_reached` avec
        `Retry-After` ; `409` `method_not_enabled` ou `method_unavailable` ; `401` `two_factor_challenge_expired`.
        Rate limit : 5 requêtes par fenêtre.""")
    @PostMapping("/challenge/mail")
    @RateLimiting(max = 5)
    public ResponseEntity<MailCodeResponse> sendMailCode(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(MailCodeResponse.sent(sendMailCode.send(challengeOf(httpRequest))));
    }

    private String challengeOf(HttpServletRequest request) {
        return cookieService.extractFromRequest(request, CookieService.TWO_FACTOR_CHALLENGE_COOKIE)
            .orElseThrow(AuthenticationException.TwoFactorChallengeExpired::new);
    }

    @Schema(description = "La méthode choisie et son code à 6 chiffres")
    record VerifyRequest(
        @Schema(description = "APP ou MAIL", example = "MAIL") @NotNull TwoFactorMethod method,
        @Schema(description = "Exactement 6 chiffres", example = "123456", pattern = "\\d{6}")
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit number") String code
    ) {}
}
