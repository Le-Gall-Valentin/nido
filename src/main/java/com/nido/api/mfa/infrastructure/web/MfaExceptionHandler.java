package com.nido.api.mfa.infrastructure.web;

import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

// Ahead of GlobalExceptionHandler's last resort, which matches Exception and would otherwise be
// picked first on an order tie — turning a declared domain error into a 500.
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class MfaExceptionHandler {

    @ExceptionHandler(MfaException.class)
    public ResponseEntity<ProblemDetail> handle(MfaException e, HttpServletRequest request) {
        MfaError error = switch (e) {
            case MfaException.UserNotFound ex               -> new MfaError(404, null, null);
            case MfaException.MethodAlreadyEnabled ex       -> new MfaError(409, "method_already_enabled", null);
            case MfaException.MethodNotEnabled ex           -> new MfaError(409, "method_not_enabled", null);
            case MfaException.MethodUnavailable ex          -> new MfaError(409, "method_unavailable", null);
            case MfaException.MethodSendsNoCode ex          -> new MfaError(400, "method_sends_no_code", null);
            case MfaException.EnrolmentNotStarted ex        -> new MfaError(422, null, null);
            case MfaException.CodeInvalid ex                -> new MfaError(401, null, null);
            case MfaException.CodeExpired ex                -> new MfaError(410, "code_expired", null);
            case MfaException.CodeSpent ex                  -> new MfaError(410, "code_spent", null);
            // No Retry-After: that is how the client tells this lockout from the rate limiter.
            case MfaException.ConfirmMaxAttemptsExceeded ex -> new MfaError(429, null, null);
            case MfaException.ResendTooSoon ex              -> new MfaError(429, "resend_too_soon", ex.seconds());
            case MfaException.SendLimitReached ex           -> new MfaError(429, "send_limit_reached", ex.seconds());
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(error.status()), e.getClass().getSimpleName(), e.getMessage(),
            URI.create(request.getRequestURI()));
        if (error.code() != null) {
            problem.setProperty("error_code", error.code());
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.status(error.status());
        if (error.retryAfterSeconds() != null) {
            problem.setProperty("retryAfterSeconds", error.retryAfterSeconds());
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(error.retryAfterSeconds()));
        }
        return response.body(problem);
    }

    private record MfaError(int status, String code, Long retryAfterSeconds) {}
}