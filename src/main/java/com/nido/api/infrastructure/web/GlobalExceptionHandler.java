package com.nido.api.infrastructure.web;

import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * The last resort, for everything the bounded contexts did not declare.
 *
 * <p>Every expected error in this API is an RFC 7807 {@code ProblemDetail}, produced by the advice
 * of the context that owns it. Anything else — a pool exhaustion, a null dereference, a constraint
 * violation — had no handler at all and fell through to Spring Boot's default error page, which
 * answers {@code {"timestamp","status","error","path"}} as {@code application/json}. The status was
 * right and nothing leaked; the shape simply stopped being the API's own exactly when a client's
 * error handling matters most. That is what this fixes — measured, not assumed:
 * {@code UnexpectedFailureIT} runs on a real port because MockMvc never performs the container's
 * error dispatch and therefore cannot see any of it.
 *
 * <p><b>The detail is deliberately fixed text.</b> {@code e.getMessage()} on an unplanned exception
 * is written for us, not for the caller — a pool address, an SQL fragment, a file path. The stack
 * goes to the log, where the operator can read it; the caller gets to know that it was our fault
 * and nothing more.
 *
 * <p><b>On catching {@code Exception}.</b> Doing it naively swallows errors that already carried a
 * meaningful status, and it does so silently — the status simply changes. Two families must keep
 * theirs, and neither was obvious from reading:
 *
 * <ul>
 *   <li>Spring MVC's own exceptions. A malformed UUID in a path is a 400, an unreadable body is a
 *       400, the SPA fallback raises a 404 for an unknown {@code /api} path. Extending
 *       {@link ResponseEntityExceptionHandler} hands every one of them back to Spring, which
 *       already answers with the right status <em>and</em> a {@code ProblemDetail} — so they gain
 *       the API's format instead of losing their meaning. Two integration tests turned red the
 *       moment this class caught {@code Exception} without it.</li>
 *   <li>The security exceptions, handed back to the very beans {@code SecurityConfig} wires into
 *       the filter chain, so a rejection looks the same whether it was decided in a filter or
 *       inside a method.</li>
 * </ul>
 *
 * <p>Order matters as much as content: this advice matches {@code Exception}, and Spring picks the
 * first advice in order with a match rather than the most specific one, so every context's advice
 * must outrank it. {@code ExceptionHandlingConventionsTest} enforces that — a tie sent a
 * {@code SpaceNotFound} back as a 500 the first time this class was wired in.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    public GlobalExceptionHandler(AuthenticationEntryPoint authenticationEntryPoint,
                                  AccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * Thrown inside the dispatch (method security) rather than in a filter, so it would otherwise
     * reach the catch-all below and turn a 401 or a 403 into a 500. Delegating to the configured
     * beans keeps one shape for both paths.
     */
    @ExceptionHandler(AuthenticationException.class)
    public void handleUnauthenticated(AuthenticationException e, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        authenticationEntryPoint.commence(request, response, e);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void handleForbidden(AccessDeniedException e, HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        accessDeniedHandler.handle(request, response, e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest request) {
        // The only place in the codebase that logs an error nobody anticipated, with the request
        // that caused it. Tomcat also logs the stack trace on its own, but without this context.
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProblemDetailFactory.of(
            HttpStatus.INTERNAL_SERVER_ERROR, "InternalError",
            "Something went wrong on our side. The incident has been logged.",
            URI.create(request.getRequestURI())));
    }
}
