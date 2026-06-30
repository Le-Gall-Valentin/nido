package com.synapsys.api.identity.infrastructure.web;

import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.shared.infrastructure.web.ProblemDetailFactory;
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
public class IdentityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IdentityExceptionHandler.class);

    @ExceptionHandler(IdentityException.class)
    public ResponseEntity<ProblemDetail> handle(IdentityException e, HttpServletRequest request) {
        IdentityErrorResponse response = switch (e) {
            case IdentityException.UserNotFound ex ->
                    response(404, ex, "User not found.");

            case IdentityException.UserNotActive ex ->
                    response(403, ex, "User account is not active.");

            case IdentityException.InsufficientPermissions ex ->
                    response(403, ex, "Insufficient permissions.");

            case IdentityException.UserAlreadyInactive ex ->
                    response(409, ex, "User account is already inactive.");

            case IdentityException.UserAlreadyActive ex ->
                    response(409, ex, "User account is already active.");

            case IdentityException.UsernameAlreadyExists ex ->
                    response(409, ex, "Username already taken.");

            case IdentityException.EmailAlreadyExists ex ->
                    response(409, ex, "Email already registered.");

            case IdentityException.InvalidCurrentPassword ex ->
                    response(422, ex, "Current password is incorrect.");

            case IdentityException.RoleAlreadyAssigned ex ->
                    response(409, ex, "User already has this role.");

            case IdentityException.DataIntegrityError ex -> {
                log.error("Data integrity violation on {}", request.getRequestURI(), ex);
                yield response(500, ex, "An unexpected error occurred. Please try again later.");
            }
        };

        ProblemDetail problem = ProblemDetailFactory.of(
                HttpStatus.valueOf(response.status()), response.title(), response.detail(),
                URI.create(request.getRequestURI()));

        return ResponseEntity.status(response.status()).body(problem);
    }

    private static IdentityErrorResponse response(int status, IdentityException e, String detail) {
        return new IdentityErrorResponse(status, e.getClass().getSimpleName(), detail);
    }

    private record IdentityErrorResponse(int status, String title, String detail) {}
}