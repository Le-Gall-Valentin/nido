package com.nido.api.notifications.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.notifications.application.port.in.ChangeNotificationPreferenceUseCase;
import com.nido.api.notifications.application.port.in.GetNotificationPreferencesUseCase;
import com.nido.api.notifications.infrastructure.web.dto.ChangeNotificationPreferenceRequest;
import com.nido.api.notifications.infrastructure.web.dto.NotificationPreferencesResponse;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The preferences card's routes: always the signed-in account's own choices, never anyone else's. */
@Tag(name = "Notifications")
@SecurityRequirement(name = "cookieAuth")
@Validated
@RestController
@RequestMapping("/api/notifications/preferences")
public class NotificationPreferencesController {

    private final GetNotificationPreferencesUseCase getPreferences;
    private final ChangeNotificationPreferenceUseCase changePreference;

    public NotificationPreferencesController(GetNotificationPreferencesUseCase getPreferences,
                                             ChangeNotificationPreferenceUseCase changePreference) {
        this.getPreferences = getPreferences;
        this.changePreference = changePreference;
    }

    @Operation(summary = "Mes préférences de notification",
        description = "Les canaux disponibles sur cette installation (aucun sans mail configuré) et tous les types de notification, chacun avec son état. Rate limit : 60 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Préférences"),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreferencesResponse> get(@Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        return ResponseEntity.ok(NotificationPreferencesResponse.of(getPreferences.get(caller.userId())));
    }

    @Operation(summary = "Activer ou couper un canal",
        description = "Couper un canal coupe toutes les notifications qui passent par lui, sans toucher aux réglages par type. Rate limit : 20 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Enregistré", content = @Content),
        @ApiResponse(responseCode = "400", description = "État absent",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Canal inconnu ou absent de cette installation",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/channels/{channel}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changeChannel(@PathVariable("channel") String channel,
                                              @Valid @RequestBody ChangeNotificationPreferenceRequest request,
                                              @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        changePreference.changeChannel(caller.userId(), channel, request.enabled());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activer ou couper un type de notification",
        description = "Le type est désigné par son code, <contexte>.<nom>. Rate limit : 20 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Enregistré", content = @Content),
        @ApiResponse(responseCode = "400", description = "État absent",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Type inconnu",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/types/{type}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changeType(@PathVariable("type") String type,
                                           @Valid @RequestBody ChangeNotificationPreferenceRequest request,
                                           @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        changePreference.changeType(caller.userId(), type, request.enabled());
        return ResponseEntity.noContent().build();
    }
}
