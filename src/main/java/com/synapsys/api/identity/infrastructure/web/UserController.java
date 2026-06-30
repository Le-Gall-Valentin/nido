package com.synapsys.api.identity.infrastructure.web;

import com.synapsys.api.identity.application.port.in.ActivateUserUseCase;
import com.synapsys.api.identity.application.port.in.AdminResetTotpUseCase;
import com.synapsys.api.identity.application.port.in.ChangeMyPasswordUseCase;
import com.synapsys.api.identity.application.port.in.DeleteUserUseCase;
import com.synapsys.api.identity.application.port.in.ListUsersUseCase;
import com.synapsys.api.identity.application.port.in.UpdateUserUseCase;
import com.synapsys.api.shared.security.AuthenticatedUser;
import com.synapsys.api.shared.security.CurrentUser;
import com.synapsys.api.identity.application.port.in.DeactivateUserUseCase;
import com.synapsys.api.identity.application.port.in.GetCurrentUserUseCase;
import com.synapsys.api.identity.application.port.in.RegisterUseCase;
import com.synapsys.api.identity.application.port.in.UpdateMyProfileUseCase;
import com.synapsys.api.identity.domain.model.ActivateUserCommand;
import com.synapsys.api.identity.domain.model.AdminResetTotpCommand;
import com.synapsys.api.identity.domain.model.ChangeMyPasswordCommand;
import com.synapsys.api.identity.domain.model.DeactivateUserCommand;
import com.synapsys.api.identity.domain.model.DeleteUserCommand;
import com.synapsys.api.identity.domain.model.RegisterCommand;
import com.synapsys.api.identity.domain.model.UpdateProfileCommand;
import com.synapsys.api.identity.domain.model.UpdateUserCommand;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.model.UserAdminView;
import com.synapsys.api.identity.domain.model.UserSelfView;
import com.synapsys.api.identity.infrastructure.web.dto.ChangePasswordRequest;
import com.synapsys.api.identity.infrastructure.web.dto.PageResponse;
import com.synapsys.api.identity.infrastructure.web.dto.RegisterRequest;
import com.synapsys.api.identity.infrastructure.web.dto.UpdateProfileRequest;
import com.synapsys.api.identity.infrastructure.web.dto.UpdateUserRequest;
import com.synapsys.api.identity.infrastructure.web.dto.UserAdminItemResponse;
import com.synapsys.api.identity.infrastructure.web.dto.UserInfoResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimitMode;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Users")
@SecurityRequirement(name = "cookieAuth")
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final RegisterUseCase registerUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final AdminResetTotpUseCase adminResetTotpUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final ChangeMyPasswordUseCase changeMyPasswordUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final ActivateUserUseCase activateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase,
                          RegisterUseCase registerUseCase,
                          DeactivateUserUseCase deactivateUserUseCase,
                          AdminResetTotpUseCase adminResetTotpUseCase,
                          UpdateMyProfileUseCase updateMyProfileUseCase,
                          ChangeMyPasswordUseCase changeMyPasswordUseCase,
                          ListUsersUseCase listUsersUseCase,
                          ActivateUserUseCase activateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          UpdateUserUseCase updateUserUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.registerUseCase = registerUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
        this.adminResetTotpUseCase = adminResetTotpUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.changeMyPasswordUseCase = changeMyPasswordUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.activateUserUseCase = activateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
    }

    @Operation(
        summary = "Profil de l'utilisateur connecté",
        description = "Retourne les informations du compte de l'utilisateur authentifié. Rate limit : 60 req/fenêtre."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Informations du compte",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserInfoResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Compte désactivé (le JWT est valide mais le compte a été désactivé entre-temps)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable (compte supprimé alors que la session était encore active)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping("/me")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> me(@Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        UserSelfView view = getCurrentUserUseCase.getCurrentUser(caller.userId());
        return ResponseEntity.ok(new UserInfoResponse(
            view.id(), view.username(), view.email(), view.role(), view.createdAt(), view.totpEnabled()));
    }

    @Operation(
        summary = "Lister les utilisateurs (admin)",
        description = """
            Retourne la liste paginée de tous les utilisateurs. Accessible aux rôles `ADMIN` et `SUPER_ADMIN`.

            Le paramètre optionnel `search` filtre les utilisateurs dont le nom d'utilisateur
            ou l'email contient la chaîne fournie (insensible à la casse).

            Rate limit : 60 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Liste paginée des utilisateurs",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Paramètre de pagination ou de tri invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Rôle insuffisant (réservé aux ADMIN et SUPER_ADMIN)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserAdminItemResponse>> listUsers(
            @Parameter(description = "Index de la page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Nombre d'éléments par page (1–100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,

            @Parameter(description = "Champ de tri", schema = @Schema(allowableValues = {"username", "email", "role", "active", "createdAt"}))
            @RequestParam(defaultValue = "createdAt") @Pattern(regexp = "username|email|role|active|createdAt", message = "must be one of: username, email, role, active, createdAt") String sortBy,

            @Parameter(description = "Direction du tri", schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc", message = "must be 'asc' or 'desc'") String sortDirection,

            @Parameter(description = "Filtre optionnel : ne retourne que les utilisateurs dont le nom d'utilisateur ou l'email contient cette chaîne (insensible à la casse)", example = "alice")
            @RequestParam(required = false) @Size(max = 254, message = "must be at most 254 characters") String search) {
        SortRequest sort = new SortRequest(sortBy, "asc".equalsIgnoreCase(sortDirection));
        PageResult<UserAdminView> result = listUsersUseCase.listUsers(page, size, sort, search);
        PageResponse<UserAdminItemResponse> response = new PageResponse<>(
            result.content().stream()
                .map(v -> new UserAdminItemResponse(
                    v.id(), v.username(), v.email(), v.role(),
                    v.isActive(), v.createdAt(), v.totpEnabled()))
                .toList(),
            result.totalElements(), result.page(), result.size()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Créer un utilisateur (admin)",
        description = """
            Crée un nouvel utilisateur. Accessible aux rôles `ADMIN` et `SUPER_ADMIN`.

            **Contrainte de rôle** : un `ADMIN` ne peut créer que des utilisateurs avec le rôle `USER`.
            Seul un `SUPER_ADMIN` peut créer un `ADMIN` ou un autre `SUPER_ADMIN`.
            Toute tentative de dépassement de rôle retourne `403`.

            Rate limit : 20 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Utilisateur créé — l'URL de la ressource est retournée dans le header `Location`",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserInfoResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Corps de requête invalide (champs manquants, format incorrect, contraintes de mot de passe non respectées)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Rôle insuffisant ou tentative de créer un utilisateur avec un rôle supérieur au sien",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Le nom d'utilisateur ou l'adresse email est déjà utilisé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        User user = registerUseCase.register(
            new RegisterCommand(request.username(), request.email(), request.password(), request.role()),
            caller.role()
        );
        URI location = URI.create("/api/users/" + user.id());
        return ResponseEntity.created(location).body(new UserInfoResponse(
            user.id(), user.username(), user.email(), user.role(), user.createdAt(), false));
    }

    @Operation(
        summary = "Supprimer un utilisateur (admin)",
        description = """
            Supprime définitivement un utilisateur. Accessible aux rôles `ADMIN` et `SUPER_ADMIN`.

            Les mêmes contraintes de rôle que la création s'appliquent : un `ADMIN` ne peut pas
            supprimer un `SUPER_ADMIN` ou un autre `ADMIN`. Retourne `403` en cas de dépassement.

            Rate limit : 20 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Utilisateur supprimé", content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Rôle insuffisant pour supprimer cet utilisateur",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @DeleteMapping("/{id}")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "UUID de l'utilisateur à supprimer") @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        deleteUserUseCase.delete(new DeleteUserCommand(id, caller.userId(), caller.role()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Activer un utilisateur (admin)",
        description = """
            Réactive un compte utilisateur précédemment désactivé. Accessible aux rôles `ADMIN` et `SUPER_ADMIN`.

            Retourne `409` si le compte est déjà actif. Les mêmes contraintes de rôle s'appliquent.

            Rate limit : 20 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Utilisateur activé", content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Le compte est déjà actif",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/{id}/activate")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "UUID de l'utilisateur à activer") @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        activateUserUseCase.activate(new ActivateUserCommand(id, caller.userId(), caller.role()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Désactiver un utilisateur (admin)",
        description = """
            Désactive un compte utilisateur. L'utilisateur ne pourra plus se connecter
            tant que son compte reste désactivé. Accessible aux rôles `ADMIN` et `SUPER_ADMIN`.

            Retourne `409` si le compte est déjà inactif. Les mêmes contraintes de rôle s'appliquent.

            Rate limit : 20 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Utilisateur désactivé", content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Le compte est déjà inactif",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/{id}/deactivate")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "UUID de l'utilisateur à désactiver") @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        deactivateUserUseCase.deactivate(new DeactivateUserCommand(id, caller.userId(), caller.role()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Modifier un utilisateur (admin)",
        description = """
            Modifie les informations d'un utilisateur. Accessible aux rôles `ADMIN` et `SUPER_ADMIN`.

            **Règles d'autorisation :**
            - Un `SUPER_ADMIN` peut modifier un `ADMIN` ou un `USER`
            - Un `ADMIN` peut modifier un `USER` uniquement
            - Il n'est pas possible de modifier son propre compte via cet endpoint
            - Il n'est pas possible d'assigner le rôle `SUPER_ADMIN`
            - Un `ADMIN` ne peut pas assigner le rôle `ADMIN` (escalade de privilèges)
            - Retourne `409` si l'utilisateur a déjà le rôle demandé
            - Retourne `403` si le compte cible est désactivé

            Rate limit : 20 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Utilisateur modifié", content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Corps de requête invalide (rôle manquant ou inconnu)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Rôle insuffisant, tentative de modification de son propre compte, ou compte cible désactivé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "L'utilisateur a déjà ce rôle",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PatchMapping("/{id}")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> updateUser(
            @Parameter(description = "UUID de l'utilisateur à modifier") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        updateUserUseCase.update(new UpdateUserCommand(id, caller.userId(), caller.role(), request.role()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Réinitialiser le TOTP d'un utilisateur (admin)",
        description = """
            Supprime le secret TOTP d'un utilisateur et désactive son authentification à deux facteurs.
            L'utilisateur devra reconfigurer le TOTP depuis son compte s'il souhaite le réactiver.

            À utiliser lorsqu'un utilisateur a perdu l'accès à son application d'authentification.

            Accessible aux rôles `ADMIN` et `SUPER_ADMIN`. Les contraintes de rôle habituelles s'appliquent.

            Rate limit : 10 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "TOTP réinitialisé", content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/{id}/2fa/reset")
    @RateLimiting(max = 10)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> resetTotp(
            @Parameter(description = "UUID de l'utilisateur dont le TOTP est à réinitialiser") @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        adminResetTotpUseCase.reset(new AdminResetTotpCommand(id, caller.userId(), caller.role()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Mettre à jour son profil",
        description = """
            Permet à l'utilisateur connecté de modifier son nom d'utilisateur et son adresse email.

            Double rate limit : 20 req/fenêtre globalement, et 5 req/fenêtre par utilisateur.
            Retourne `409` si le nouveau nom d'utilisateur ou la nouvelle adresse email est déjà pris.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Profil mis à jour", content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Corps invalide (champs manquants ou non conformes aux contraintes)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Compte désactivé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Nom d'utilisateur ou email déjà utilisé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes (global ou par utilisateur)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PatchMapping("/me")
    @RateLimiting(max = 20)
    @RateLimiting(mode = RateLimitMode.USER, max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                              @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        updateMyProfileUseCase.updateProfile(
            new UpdateProfileCommand(caller.userId(), request.username(), request.email()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Changer son mot de passe",
        description = """
            Permet à l'utilisateur connecté de modifier son mot de passe.
            Le mot de passe actuel est requis pour confirmer l'opération.
            Le nouveau mot de passe doit être différent de l'actuel.

            **Contraintes du nouveau mot de passe** : 8–72 caractères, au moins une majuscule,
            un chiffre et un caractère spécial.

            Double rate limit : 10 req/fenêtre globalement, et 5 req/fenêtre par utilisateur.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mot de passe modifié", content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Corps invalide (contraintes non respectées ou nouveau mot de passe identique à l'actuel)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Compte désactivé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Mot de passe actuel incorrect",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes (global ou par utilisateur)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PatchMapping("/me/password")
    @RateLimiting(max = 10)
    @RateLimiting(mode = RateLimitMode.USER, max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        changeMyPasswordUseCase.changeMyPassword(
            new ChangeMyPasswordCommand(caller.userId(), request.currentPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }
}