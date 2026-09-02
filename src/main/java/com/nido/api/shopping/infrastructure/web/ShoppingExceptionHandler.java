package com.nido.api.shopping.infrastructure.web;

import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class ShoppingExceptionHandler {

    @ExceptionHandler(ShoppingException.class)
    public ResponseEntity<ProblemDetail> handle(ShoppingException e, HttpServletRequest request) {
        ShoppingErrorResponse response = switch (e) {
            case ShoppingException.CategoryNotFound ignored -> new ShoppingErrorResponse(404, "Category not found.");
            case ShoppingException.ItemNotFound ignored -> new ShoppingErrorResponse(404, "Item not found.");
            case ShoppingException.CannotDeleteFallbackCategory ignored ->
                new ShoppingErrorResponse(400, "Cannot delete the fallback category.");
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), e.getClass().getSimpleName(), response.detail(),
            URI.create(request.getRequestURI()));
        return ResponseEntity.status(response.status()).body(problem);
    }

    private record ShoppingErrorResponse(int status, String detail) {}
}
