package com.nido.api.kitchen.infrastructure.web;

import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class KitchenExceptionHandler {

    @ExceptionHandler(KitchenException.class)
    public ResponseEntity<ProblemDetail> handle(KitchenException e, HttpServletRequest request) {
        String detail = switch (e) {
            case KitchenException.RecipeNotFound ignored -> "Recipe not found.";
            case KitchenException.MenuEntryNotFound ignored -> "Menu entry not found.";
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.NOT_FOUND, e.getClass().getSimpleName(), detail, URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
