package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class FinanceExceptionHandler {

    @ExceptionHandler(FinanceException.class)
    public ResponseEntity<ProblemDetail> handle(FinanceException e, HttpServletRequest request) {
        FinanceErrorResponse response = switch (e) {
            case FinanceException.TransactionNotFound ignored -> new FinanceErrorResponse(404, "Transaction not found.");
            case FinanceException.RecurringSeriesNotFound ignored -> new FinanceErrorResponse(404, "Recurring series not found.");
            case FinanceException.CategoryNotFound ignored -> new FinanceErrorResponse(404, "Category not found.");
            case FinanceException.CategoryInUse ignored -> new FinanceErrorResponse(409, "Category is still used by existing transactions.");
            case FinanceException.SameSpaceTransfer ignored -> new FinanceErrorResponse(400, "Cannot transfer a transaction into its own context.");
            case FinanceException.InvalidContributionShares ignored -> new FinanceErrorResponse(400, "Contribution shares must sum to the transaction amount.");
            case FinanceException.PayerRequired ignored -> new FinanceErrorResponse(400, "A payer is required when the transaction has contributors.");
            case FinanceException.SavingsGoalNotFound ignored -> new FinanceErrorResponse(404, "Savings goal not found.");
            case FinanceException.DecryptionFailed ignored -> new FinanceErrorResponse(500, "Could not process the requested finance data.");
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), e.getClass().getSimpleName(), response.detail(),
            URI.create(request.getRequestURI()));
        return ResponseEntity.status(response.status()).body(problem);
    }

    private record FinanceErrorResponse(int status, String detail) {}
}
