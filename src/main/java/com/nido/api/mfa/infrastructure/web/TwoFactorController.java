package com.nido.api.mfa.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.mfa.application.port.in.ManageTwoFactorMethodsUseCase;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.infrastructure.web.dto.AppSetupResponse;
import com.nido.api.mfa.infrastructure.web.dto.DisableMethodRequest;
import com.nido.api.mfa.infrastructure.web.dto.MailSetupResponse;
import com.nido.api.mfa.infrastructure.web.dto.MethodStateResponse;
import com.nido.api.mfa.infrastructure.web.dto.ResendResponse;
import com.nido.api.mfa.infrastructure.web.dto.SetupResponse;
import com.nido.api.mfa.infrastructure.web.dto.TwoFactorCodeRequest;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "2FA - Management")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorController {

    private final ManageTwoFactorMethodsUseCase methods;

    public TwoFactorController(ManageTwoFactorMethodsUseCase methods) {
        this.methods = methods;
    }

    @Operation(summary = "Méthodes de double authentification du compte", description = """
        Chaque méthode, dans l'ordre `APP` puis `MAIL`, avec `enabled` (activée sur le compte) et `usable`
        (utilisable maintenant : `MAIL` ne l'est pas tant que l'envoi de mails est coupé). Activée mais
        inutilisable = en pause : elle n'est pas demandée à la connexion. Rate limit : 30 req/fenêtre.""")
    @GetMapping
    @RateLimiting(max = 30)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MethodStateResponse>> list(@Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        return ResponseEntity.ok(methods.methods(caller.userId()).stream().map(MethodStateResponse::of).toList());
    }

    @Operation(summary = "Commencer l'activation d'une méthode (`app` ou `mail`)", description = """
        `app` : renvoie le secret et l'URI `otpauth://` à scanner. Un second appel renvoie le même secret tant
        que l'activation n'est pas confirmée (deux onglets ne montrent pas deux QR codes).

        `mail` : envoie un code à 6 chiffres à l'adresse du compte, valable 10 minutes, et renvoie
        `{ sentTo, resendAfterSeconds }`. Rappeler la route envoie un nouveau code, pas avant 60 s.

        `409` `method_already_enabled` (déjà active) ou `method_unavailable` (envoi de mails coupé) ;
        `429` `resend_too_soon` ou `send_limit_reached`, avec `Retry-After` ; `404` pour une autre méthode.
        Rate limit : 5 req/fenêtre.""")
    @ApiResponse(responseCode = "200", description = "`app` : le secret à scanner ; `mail` : le code est parti",
        content = @Content(mediaType = "application/json",
            schema = @Schema(oneOf = {AppSetupResponse.class, MailSetupResponse.class})))
    @PostMapping("/{method}/setup")
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SetupResponse> setup(@PathVariable("method") String method,
                                               @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        EnrolmentStarted started = methods.startEnrolment(caller.userId(), parse(method));
        return ResponseEntity.ok(switch (started) {
            case EnrolmentStarted.AppEnrolment app -> new AppSetupResponse(app.otpauthUri(), app.secret());
            case EnrolmentStarted.MailEnrolment mail -> new MailSetupResponse(mail.sentTo(), mail.resendAfterSeconds());
        });
    }

    @Operation(summary = "Confirmer l'activation d'une méthode", description = """
        Le code de l'application, ou celui reçu par mail. `401` si le code est faux ; `422` si aucune activation
        n'est en cours (jamais commencée ou expirée) ; `429` sans `Retry-After` après 5 codes faux : l'activation
        est annulée, il faut la recommencer. `409` comme pour `/setup`. Rate limit : 10 req/fenêtre.""")
    @ApiResponse(responseCode = "204", description = "Méthode activée", content = @Content)
    @PostMapping("/{method}/confirm")
    @RateLimiting(max = 10)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirm(@PathVariable("method") String method, @Valid @RequestBody TwoFactorCodeRequest request,
                                        @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        methods.confirmEnrolment(caller.userId(), parse(method), request.code());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Envoyer le code qui désactive le code par mail", description = """
        `mail` seulement : envoie un code à l'adresse du compte. `400` `method_sends_no_code` pour `app` (son
        code vient de l'application) ; `409` `method_not_enabled` ou `method_unavailable` ; `429` comme
        `/setup`. Rate limit : 5 req/fenêtre.""")
    @PostMapping("/{method}/disable-code")
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResendResponse> disableCode(@PathVariable("method") String method,
                                                      @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        return ResponseEntity.ok(new ResendResponse(methods.sendDisableCode(caller.userId(), parse(method))));
    }

    @Operation(summary = "Désactiver une méthode", description = """
        Demande le code de la méthode (de l'application, ou reçu par mail après `/disable-code`). Une méthode en
        pause (`MAIL` pendant que l'envoi de mails est coupé) se désactive sans code. `401` si le code est faux ou
        manque ; `410` `code_expired` si aucun code n'attend plus (expiré, jamais demandé) ou `code_spent` après le
        cinquième code faux — pour `mail` il faut en demander un autre, pour `app` attendre un quart d'heure ;
        `409` `method_not_enabled`. Un mail prévient le compte. Rate limit : 5 req/fenêtre.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Méthode désactivée", content = @Content),
        @ApiResponse(responseCode = "401", description = "Code faux ou manquant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Méthode pas activée (`method_not_enabled`)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "410",
            description = "Plus de code en attente (`code_expired`), ou cinq codes faux (`code_spent`)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{method}")
    @RateLimiting(max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disable(@PathVariable("method") String method,
                                        @Valid @RequestBody(required = false) DisableMethodRequest request,
                                        @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        methods.disable(caller.userId(), parse(method), request == null ? null : request.code());
        return ResponseEntity.noContent().build();
    }

    private static TwoFactorMethod parse(String segment) {
        return TwoFactorMethod.fromPathSegment(segment)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such two-factor method"));
    }
}
