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
            case FinanceException.InvalidContributionShares ignored -> new FinanceErrorResponse(400, "Contribution shares must sum to the transaction amount.");
            case FinanceException.PayerRequired ignored -> new FinanceErrorResponse(400, "A payer is required when the transaction has contributors.");
            case FinanceException.InvalidEndDate ignored -> new FinanceErrorResponse(400, "The end date must be on or after the anchor date.");
            case FinanceException.NotAPartyToSettlement ignored -> new FinanceErrorResponse(403, "Only the debtor or the creditor can settle this debt.");
            case FinanceException.SavingsGoalNotFound ignored -> new FinanceErrorResponse(404, "Savings goal not found.");
            case FinanceException.InvalidSavingsGoalAppearance ignored -> new FinanceErrorResponse(422, "Color or glyph outside the allowed palette.");
            case FinanceException.ContributionExceedsGoalTarget ignored -> new FinanceErrorResponse(422, "This contribution would exceed the goal's target amount.");
            case FinanceException.DecryptionFailed ignored -> new FinanceErrorResponse(500, "Could not process the requested finance data.");
            case FinanceException.MemberNotInSpace ignored -> new FinanceErrorResponse(404, "Member is not part of this space.");
            case FinanceException.SettlementExceedsDebt ignored -> new FinanceErrorResponse(422, "This settlement would exceed the amount actually owed.");
            case FinanceException.TargetAmountBelowContributed ignored -> new FinanceErrorResponse(422, "The target amount cannot be lowered below what has already been contributed.");
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(response.status()), e.getClass().getSimpleName(), response.detail(),
            URI.create(request.getRequestURI()));
        return ResponseEntity.status(response.status()).body(problem);
    }

    private record FinanceErrorResponse(int status, String detail) {}
}
