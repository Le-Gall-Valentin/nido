package com.nido.api.identity.infrastructure.web;

import com.nido.api.identity.application.port.in.ActivateUserUseCase;
import com.nido.api.identity.application.port.in.AdminResetTwoFactorUseCase;
import com.nido.api.identity.application.port.in.ChangeMyLanguageUseCase;
import com.nido.api.identity.application.port.in.ChangeMyPasswordUseCase;
import com.nido.api.identity.application.port.in.DeleteUserUseCase;
import com.nido.api.identity.application.port.in.ListUsersUseCase;
import com.nido.api.identity.application.port.in.ResendInvitationUseCase;
import com.nido.api.identity.application.port.in.UpdateUserUseCase;
import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import com.nido.api.identity.application.port.in.DeactivateUserUseCase;
import com.nido.api.identity.application.port.in.GetCurrentUserUseCase;
import com.nido.api.identity.application.port.in.RegisterUseCase;
import com.nido.api.identity.application.port.in.UpdateMyProfileUseCase;
import com.nido.api.identity.domain.model.ActivateUserCommand;
import com.nido.api.identity.domain.model.AdminResetTwoFactorCommand;
import com.nido.api.identity.domain.model.ChangeMyPasswordCommand;
import com.nido.api.identity.domain.model.DeactivateUserCommand;
import com.nido.api.identity.domain.model.DeleteUserCommand;
import com.nido.api.identity.domain.model.ProfileUpdate;
import com.nido.api.identity.domain.model.RegisterCommand;
import com.nido.api.identity.domain.model.RegisteredAccount;
import com.nido.api.identity.domain.model.ResendInvitationCommand;
import com.nido.api.identity.domain.model.UpdateProfileCommand;
import com.nido.api.identity.domain.model.UpdateUserCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.model.UserAdminView;
import com.nido.api.identity.domain.model.UserSelfView;
import com.nido.api.identity.infrastructure.web.dto.ChangePasswordRequest;
import com.nido.api.identity.infrastructure.web.dto.InvitationDeliveryResponse;
import com.nido.api.identity.infrastructure.web.dto.InvitationStateResponse;
import com.nido.api.identity.infrastructure.web.dto.PageResponse;
import com.nido.api.identity.infrastructure.web.dto.ProfileUpdateResponse;
import com.nido.api.identity.infrastructure.web.dto.RegisterRequest;
import com.nido.api.identity.infrastructure.web.dto.RegisteredUserResponse;
import com.nido.api.identity.infrastructure.web.dto.ResetTwoFactorRequest;
import com.nido.api.identity.infrastructure.web.dto.UpdateLanguageRequest;
import com.nido.api.identity.infrastructure.web.dto.UpdateProfileRequest;
import com.nido.api.identity.infrastructure.web.dto.UpdateUserRequest;
import com.nido.api.identity.infrastructure.web.dto.UserAdminItemResponse;
import com.nido.api.identity.infrastructure.web.dto.UserInfoResponse;
import com.nido.api.infrastructure.ratelimit.RateLimitMode;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.shared.model.PageResult;
import com.nido.api.shared.model.SortRequest;
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
import java.util.List;
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
    private final AdminResetTwoFactorUseCase adminResetTwoFactorUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final ChangeMyPasswordUseCase changeMyPasswordUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final ActivateUserUseCase activateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ChangeMyLanguageUseCase changeMyLanguageUseCase;
    private final ResendInvitationUseCase resendInvitationUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase,
                          RegisterUseCase registerUseCase,
                          DeactivateUserUseCase deactivateUserUseCase,
                          AdminResetTwoFactorUseCase adminResetTwoFactorUseCase,
                          UpdateMyProfileUseCase updateMyProfileUseCase,
                          ChangeMyPasswordUseCase changeMyPasswordUseCase,
                          ListUsersUseCase listUsersUseCase,
                          ActivateUserUseCase activateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          ChangeMyLanguageUseCase changeMyLanguageUseCase,
                          ResendInvitationUseCase resendInvitationUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.registerUseCase = registerUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
        this.adminResetTwoFactorUseCase = adminResetTwoFactorUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.changeMyPasswordUseCase = changeMyPasswordUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.activateUserUseCase = activateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.changeMyLanguageUseCase = changeMyLanguageUseCase;
        this.resendInvitationUseCase = resendInvitationUseCase;
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
            view.id(), view.username(), view.email(), view.role(), view.createdAt(),
            TwoFactorMethod.ordered(view.twoFactorMethods()),
            view.language() == null ? null : view.language().code()));
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
                    v.isActive(), v.createdAt(), TwoFactorMethod.ordered(v.twoFactorMethods()), InvitationStateResponse.of(v.invitation())))
                .toList(),
            result.totalElements(), result.page(), result.size()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Créer un utilisateur (admin)",
        description = """
            Crée un compte et l'invite : la personne choisit elle-même son mot de passe par un lien valable 7 jours.
            Le lien part par mail si l'envoi de mails est configuré (`invitation.delivery` = `mail`) ; sinon il est
            rendu dans la réponse (`invitation.delivery` = `link`, `invitation.link`) pour que l'administrateur le
            transmette — il n'est jamais réaffiché. Accessible aux rôles `ADMIN` et `SUPER_ADMIN`.

            **Contrainte de rôle** : un `ADMIN` ne peut créer que des utilisateurs avec le rôle `USER`.
            Seul un `SUPER_ADMIN` peut créer un `ADMIN` ou un autre `SUPER_ADMIN`.
            Toute tentative de dépassement de rôle retourne `403`.

            Rate limit : 20 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Utilisateur créé et invité — l'URL de la ressource est retournée dans le header `Location`",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisteredUserResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Corps de requête invalide (champs manquants ou format incorrect)",
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
    public ResponseEntity<RegisteredUserResponse> register(@Valid @RequestBody RegisterRequest request,
                                                           @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        RegisteredAccount created = registerUseCase.register(
            new RegisterCommand(request.username(), request.email(), request.role()), caller.userId(), caller.role());
        User user = created.user();
        URI location = URI.create("/api/users/" + user.id());
        return ResponseEntity.created(location).body(new RegisteredUserResponse(
            user.id(), user.username(), user.email(), user.role(), user.createdAt(), List.of(), null,
            InvitationDeliveryResponse.of(created.invitation())));
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
        summary = "Réinitialiser la double authentification d'un compte (admin)",
        description = """
            Retire les méthodes cochées (`APP`, `MAIL`) ; une méthode demandée mais inactive est ignorée, et une
            activation en cours de cette méthode est effacée. À utiliser quand quelqu'un a perdu son téléphone ou
            l'accès à sa boîte mail. Un mail prévient le compte, et une notification les super-admins quand c'est un
            `ADMIN` qui agit — seulement si quelque chose a été retiré. Les contraintes de rôle habituelles
            s'appliquent ; jamais sur soi-même. `400` si `methods` est vide. Rate limit : 10 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Méthodes cochées retirées", content = @Content),
        @ApiResponse(
            responseCode = "400",
            description = "Aucune méthode cochée",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
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
    public ResponseEntity<Void> resetTwoFactor(
            @Parameter(description = "UUID du compte") @PathVariable UUID id,
            @Valid @RequestBody ResetTwoFactorRequest request,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        adminResetTwoFactorUseCase.reset(new AdminResetTwoFactorCommand(id, caller.userId(), caller.role(), request.methods()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Renvoyer l'invitation d'un compte (admin)",
        description = """
            Émet un nouveau lien d'invitation, valable 7 jours, pour un compte qui n'a pas encore choisi son mot
            de passe ; l'ancien lien ne marche plus. Envoyé par mail si l'envoi de mails est configuré, sinon rendu
            dans la réponse. Mêmes contraintes de rôle que les autres gestes ; le compte doit être actif.

            Rate limit : 10 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation renouvelée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = InvitationDeliveryResponse.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant, son propre compte, ou compte désactivé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Le compte a déjà choisi son mot de passe",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/invitation")
    @RateLimiting(max = 10)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<InvitationDeliveryResponse> resendInvitation(
            @Parameter(description = "UUID du compte invité") @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        return ResponseEntity.ok(InvitationDeliveryResponse.of(
            resendInvitationUseCase.resend(new ResendInvitationCommand(id, caller.userId(), caller.role()))));
    }

    @Operation(
        summary = "Mettre à jour son profil",
        description = """
            Permet à l'utilisateur connecté de modifier son nom d'utilisateur et son adresse email.

            Double rate limit : 20 req/fenêtre globalement, et 5 req/fenêtre par utilisateur.
            Retourne `409` si le nouveau nom d'utilisateur ou la nouvelle adresse email est déjà pris.
            Changer l'adresse (hors casse) exige `currentPassword` (`400` s'il manque, `422` s'il est faux) ;
            l'ancienne adresse en est prévenue par mail.

            Si la double authentification par mail est active, un changement d'adresse répond d'abord **202**
            `{ emailCodeRequired, sentTo, resendAfterSeconds }` sans rien enregistrer : un code est parti à la
            nouvelle adresse ; la même requête avec `emailCode` enregistre. `400` `email_code_invalid` si le code est
            faux, `email_code_expired` s'il n'y en a plus en attente (expiré, jamais demandé), `email_code_spent` après
            le cinquième code faux ; `429` `resend_too_soon` ou `send_limit_reached` (avec `Retry-After`).

            Si l'envoi de mails est coupé à ce moment, rien ne peut prouver la nouvelle adresse : elle est enregistrée
            et le code par mail est retiré — **200** `{ mailMethodRemoved: true }`.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Profil mis à jour", content = @Content),
        @ApiResponse(
            responseCode = "200",
            description = "Profil mis à jour pendant que l'envoi de mails est coupé : le code par mail est retiré",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfileUpdateResponse.MailMethodRemoved.class))
        ),
        @ApiResponse(
            responseCode = "202",
            description = "Rien n'est enregistré : un code est parti à la nouvelle adresse (double authentification par mail active)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProfileUpdateResponse.EmailCodeSent.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Corps invalide (champs manquants ou non conformes aux contraintes), adresse changée sans le mot de passe actuel (`CurrentPasswordRequired`), ou code de la nouvelle adresse faux (`email_code_invalid`), plus en attente (`email_code_expired`) ou épuisé par cinq codes faux (`email_code_spent`)",
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
            responseCode = "422",
            description = "Mot de passe actuel incorrect",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Trop de requêtes (global ou par utilisateur), ou code de la nouvelle adresse refusé : demandé à nouveau trop tôt (`resend_too_soon`) ou plafond d'envois atteint (`send_limit_reached`), avec `Retry-After` et `retryAfterSeconds`",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PatchMapping("/me")
    @RateLimiting(max = 20)
    @RateLimiting(mode = RateLimitMode.USER, max = 5)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileUpdateResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                               @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        ProfileUpdate update = updateMyProfileUseCase.updateProfile(new UpdateProfileCommand(
            caller.userId(), request.username(), request.email(), request.currentPassword(), request.emailCode()));
        return switch (update) {
            case ProfileUpdate.Saved saved -> ResponseEntity.noContent().build();
            case ProfileUpdate.SavedMailMethodRemoved removed ->
                ResponseEntity.ok(new ProfileUpdateResponse.MailMethodRemoved(true));
            case ProfileUpdate.EmailCodeSent sent ->
                ResponseEntity.accepted().body(new ProfileUpdateResponse.EmailCodeSent(true, sent.sentTo(), sent.resendAfterSeconds()));
        };
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

    @Operation(
        summary = "Changer la langue de son compte",
        description = """
            Enregistre la langue dans laquelle l'utilisateur lit l'application (`fr` ou `en`).
            Une fois connecté, cette langue l'emporte sur celle que détecte chaque appareil ; c'est aussi
            celle des mails que Nido lui envoie.

            Rate limit : 20 req/fenêtre.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Langue enregistrée", content = @Content),
        @ApiResponse(responseCode = "400", description = "Langue absente ou non prise en charge",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Compte désactivé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/me/language")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changeLanguage(@Valid @RequestBody UpdateLanguageRequest request,
                                               @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        changeMyLanguageUseCase.changeLanguage(caller.userId(), Language.fromCode(request.language()).orElseThrow());
        return ResponseEntity.noContent().build();
    }
}
