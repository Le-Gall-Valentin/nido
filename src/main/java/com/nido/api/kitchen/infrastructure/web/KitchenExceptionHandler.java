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
        KitchenErrorResponse response = switch (e) {
            case KitchenException.RecipeNotFound ignored -> new KitchenErrorResponse(404, "Recipe not found.");
            case KitchenException.MenuEntryNotFound ignored -> new KitchenErrorResponse(404, "Menu entry not found.");
            case KitchenException.SameSpaceTransfer ignored ->
                new KitchenErrorResponse(400, "Cannot transfer an item into its own context.");
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), e.getClass().getSimpleName(), response.detail(),
            URI.create(request.getRequestURI()));
        return ResponseEntity.status(response.status()).body(problem);
    }

    private record KitchenErrorResponse(int status, String detail) {}
}
