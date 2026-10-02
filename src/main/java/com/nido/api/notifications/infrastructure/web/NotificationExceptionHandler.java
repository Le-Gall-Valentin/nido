package com.nido.api.notifications.infrastructure.web;

import com.nido.api.notifications.domain.model.NotificationException;
import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

// Ahead of GlobalExceptionHandler's last resort, which matches Exception and would otherwise be
// picked first on an order tie — turning a declared domain error into a 500.
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ProblemDetail> handle(NotificationException e, HttpServletRequest request) {
        String detail = switch (e) {
            case NotificationException.UnknownChannel ignored -> "No such notification channel on this installation.";
            case NotificationException.UnknownType ignored -> "No such notification type.";
        };
        ProblemDetail problem = ProblemDetailFactory.of(HttpStatus.NOT_FOUND, e.getClass().getSimpleName(), detail,
            URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
