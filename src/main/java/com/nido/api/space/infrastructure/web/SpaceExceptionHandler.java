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

@RestControllerAdvice
public class SpaceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SpaceExceptionHandler.class);

    @ExceptionHandler(SpaceException.class)
    public ResponseEntity<ProblemDetail> handle(SpaceException e, HttpServletRequest request) {
        SpaceErrorResponse response = switch (e) {
            // 404 indistinguable : même statut, même titre, même detail pour « inexistant »
            // et « pas membre ». Le titre est forcé, car le nom de classe trahirait le cas.
            case SpaceException.SpaceNotFound ex -> response(404, ex, "Space not found.");
            case SpaceException.NotAMember ignored -> new SpaceErrorResponse(
                404, SpaceException.SpaceNotFound.class.getSimpleName(), "Space not found.");
            case SpaceException.MemberNotFound ex -> response(404, ex, "Member not found in this space.");

            case SpaceException.InsufficientRole ex -> response(403, ex, "Your role in this space does not allow this action.");
            case SpaceException.OwnerRequired ex -> response(403, ex, "Only the owner can perform this action.");

            case SpaceException.SelfManagementForbidden ex -> response(409, ex, "You cannot manage your own membership.");
            case SpaceException.RoleAlreadyAssigned ex -> response(409, ex, "Member already has this role.");
            case SpaceException.LastOwnerCannotLeave ex -> response(409, ex, "Transfer ownership before leaving this space.");
            case SpaceException.SpaceNotEmpty ex -> response(409, ex, "This space still has members.");
            case SpaceException.AlreadyMember ex -> response(409, ex, "This user is already a member.");
            case SpaceException.PersonalSpaceAlreadyExists ex -> response(409, ex, "This account already has a personal space.");

            case SpaceException.PersonalSpaceImmutable ex -> response(422, ex, "The personal space cannot be renamed, shared or deleted.");
            case SpaceException.InvalidAppearance ex -> response(422, ex, "Accent or glyph outside the allowed palette.");
            case SpaceException.InvalidSpaceName ex -> response(422, ex, "Space name must be between 1 and 80 characters.");
            case SpaceException.InvalidSpaceDescription ex -> response(422, ex, "Space description must not exceed 280 characters.");
            case SpaceException.OwnerRoleNotAssignable ex -> response(422, ex, "The owner role is only reachable through an ownership transfer.");

            case SpaceException.DataIntegrityError ex -> {
                log.error("Data integrity violation on {}", request.getRequestURI(), ex);
                yield response(500, ex, "An unexpected error occurred. Please try again later.");
            }
        };

        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), response.title(), response.detail(),
            URI.create(request.getRequestURI()));

        return ResponseEntity.status(response.status()).body(problem);
    }

    private static SpaceErrorResponse response(int status, SpaceException e, String detail) {
        return new SpaceErrorResponse(status, e.getClass().getSimpleName(), detail);
    }

    private record SpaceErrorResponse(int status, String title, String detail) {}
}
