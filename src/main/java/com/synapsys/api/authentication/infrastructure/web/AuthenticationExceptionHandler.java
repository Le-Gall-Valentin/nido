package com.synapsys.api.authentication.infrastructure.web;

import com.synapsys.api.authentication.domain.model.AuthenticationException;
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
public class AuthenticationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handle(AuthenticationException e, HttpServletRequest request) {
        AuthErrorResponse response = switch (e) {
            case AuthenticationException.InvalidCredentials ignored ->
                    response(401, "Invalid credentials");

            case AuthenticationException.UserNotActive ignored ->
                    response(401, "Authentication required");

            case AuthenticationException.UserNotFound ignored ->
                    response(401, "Authentication required");

            case AuthenticationException.TokenExpired ignored ->
                    response(401, "Authentication required");

            case AuthenticationException.TokenNotFound ignored ->
                    response(401, "Authentication required");

            case AuthenticationException.TokenRevoked ignored ->
                    response(401, "Authentication required");

            case AuthenticationException.TotpCodeInvalid ignored ->
                    response(401, "Authentication required");

            case AuthenticationException.TotpChallengeExpired ignored ->
                    response(401, "Authentication required", "totp_challenge_expired");

            case AuthenticationException.TotpMaxAttemptsExceeded ignored ->
                    response(429, "Authentication required", null);

            case AuthenticationException.InvalidCurrentPassword ignored ->
                    response(422, "Current password is incorrect.");

            case AuthenticationException.DataIntegrityError ex -> {
                log.error("Data integrity violation on {}", request.getRequestURI(), ex);
                yield response(500, "An unexpected error occurred. Please try again later.");
            }
        };

        ProblemDetail problem = ProblemDetailFactory.of(
                HttpStatus.valueOf(response.status()), response.title(), response.detail(),
                URI.create(request.getRequestURI()));

        if (response.errorCode() != null) {
            problem.setProperty("error_code", response.errorCode());
        }

        return ResponseEntity.status(response.status()).body(problem);
    }

    private static AuthErrorResponse response(int status, String detail) {
        return new AuthErrorResponse(status, "AuthenticationError", detail, null);
    }

    private static AuthErrorResponse response(int status, String detail, String errorCode) {
        return new AuthErrorResponse(status, "AuthenticationError", detail, errorCode);
    }

    private record AuthErrorResponse(int status, String title, String detail, String errorCode) {}
}