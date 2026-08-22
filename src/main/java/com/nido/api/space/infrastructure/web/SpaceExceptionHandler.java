package com.nido.api.space.infrastructure.web;

import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import com.nido.api.space.domain.model.SpaceException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.regex.Pattern;

@RestControllerAdvice
public class SpaceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SpaceExceptionHandler.class);

    // Sur cette route, l'identifiant fait partie du chemin : sans ce masquage, le champ
    // "instance" trahirait à lui seul lequel des deux cas indistinguables (id inconnu / id
    // adressé à autrui) s'est produit, même si statut, titre et detail restent identiques.
    private static final Pattern ACCEPT_INVITATION_BY_ID_PATH =
        Pattern.compile("^/api/invitations/[0-9a-fA-F-]{36}/accept$");

    @ExceptionHandler(SpaceException.class)
    public ResponseEntity<ProblemDetail> handle(SpaceException e, HttpServletRequest request) {
        SpaceErrorResponse response = switch (e) {
            // 404 indistinguable : même statut, même titre, même detail pour « inexistant »
            // et « pas membre ». Le titre est forcé, car le nom de classe trahirait le cas.
            case SpaceException.SpaceNotFound ex -> response(404, ex, "Space not found.");
            case SpaceException.NotAMember ignored -> new SpaceErrorResponse(
                404, SpaceException.SpaceNotFound.class.getSimpleName(), "Space not found.");
            case SpaceException.MemberNotFound ex -> response(404, ex, "Member not found in this space.");
            case SpaceException.InvitationNotFound ex -> response(404, ex, "Invitation not found.");
            // Indistinguable d'un code inconnu : sinon un appelant pourrait, en soumettant
            // des codes au hasard, apprendre lesquels existent réellement.
            case SpaceException.InvitationEmailMismatch ignored -> new SpaceErrorResponse(
                404, SpaceException.InvitationNotFound.class.getSimpleName(), "Invitation not found.");

            case SpaceException.InsufficientRole ex -> response(403, ex, "Your role in this space does not allow this action.");
            case SpaceException.OwnerRequired ex -> response(403, ex, "Only the owner can perform this action.");

            case SpaceException.SelfManagementForbidden ex -> response(409, ex, "You cannot manage your own membership.");
            case SpaceException.OwnerMembershipProtected ex -> response(409, ex, "The owner's membership is protected.");
            case SpaceException.RoleAlreadyAssigned ex -> response(409, ex, "Member already has this role.");
            case SpaceException.LastOwnerCannotLeave ex -> response(409, ex, "Transfer ownership before leaving this space.");
            case SpaceException.SpaceNotEmpty ex -> response(409, ex, "This space still has members.");
            case SpaceException.AlreadyMember ex -> response(409, ex, "This user is already a member.");
            case SpaceException.PersonalSpaceAlreadyExists ex -> response(409, ex, "This account already has a personal space.");
            case SpaceException.OwnerAlreadyExists ex -> response(409, ex, "This space already has an owner.");
            case SpaceException.InvitationNotPending ex -> response(409, ex, "This invitation is no longer pending.");
            case SpaceException.InvitationAlreadyPending ex -> response(409, ex, "This address already has a pending invitation to this space.");

            case SpaceException.PersonalSpaceImmutable ex -> response(422, ex, "The personal space cannot be renamed, shared or deleted.");
            case SpaceException.InvalidAppearance ex -> response(422, ex, "Accent or glyph outside the allowed palette.");
            case SpaceException.InvalidSpaceName ex -> response(422, ex, "Space name must be between 1 and 80 characters.");
            case SpaceException.InvalidSpaceDescription ex -> response(422, ex, "Space description must not exceed 280 characters.");
            case SpaceException.OwnerRoleNotAssignable ex -> response(422, ex, "The owner role is only reachable through an ownership transfer.");
            case SpaceException.InvitationExpired ex -> response(422, ex, "This invitation has expired.");
            case SpaceException.NoAccountForEmail ex -> response(422, ex, "No account exists for this address. Ask an administrator to create one.");

            case SpaceException.DataIntegrityError ex -> {
                log.error("Data integrity violation on {}", request.getRequestURI(), ex);
                yield response(500, ex, "An unexpected error occurred. Please try again later.");
            }
        };

        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), response.title(), response.detail(),
            instance(request));

        return ResponseEntity.status(response.status()).body(problem);
    }

    private static URI instance(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (ACCEPT_INVITATION_BY_ID_PATH.matcher(uri).matches()) {
            return URI.create("/api/invitations/%7BinvitationId%7D/accept");
        }
        return URI.create(uri);
    }

    private static SpaceErrorResponse response(int status, SpaceException e, String detail) {
        return new SpaceErrorResponse(status, e.getClass().getSimpleName(), detail);
    }

    private record SpaceErrorResponse(int status, String title, String detail) {}
}
