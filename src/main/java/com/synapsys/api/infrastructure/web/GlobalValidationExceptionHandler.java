package com.synapsys.api.infrastructure.web;

import com.synapsys.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        String details = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.BAD_REQUEST, "Validation failed", details,
            URI.create(request.getRequestURI()));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException e,
                                                                HttpServletRequest request) {
        String details = e.getAllErrors().stream()
            .map(err -> err.getDefaultMessage())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.BAD_REQUEST, "Validation failed", details,
            URI.create(request.getRequestURI()));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException e,
                                                                   HttpServletRequest request) {
        String details = e.getConstraintViolations().stream()
            .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
            .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.BAD_REQUEST, "Validation failed", details,
            URI.create(request.getRequestURI()));
        return ResponseEntity.badRequest().body(problem);
    }
}