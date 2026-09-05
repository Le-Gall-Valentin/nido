package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.application.port.in.CreateTransactionUseCase;
import com.nido.api.finance.application.port.in.DeleteTransactionUseCase;
import com.nido.api.finance.application.port.in.ListTransactionsUseCase;
import com.nido.api.finance.application.port.in.MoveTransactionUseCase;
import com.nido.api.finance.application.port.in.UpdateTransactionUseCase;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.infrastructure.web.dto.ContributionRequest;
import com.nido.api.finance.infrastructure.web.dto.CreateTransactionRequest;
import com.nido.api.finance.infrastructure.web.dto.MoveTransactionRequest;
import com.nido.api.finance.infrastructure.web.dto.TransactionResponse;
import com.nido.api.finance.infrastructure.web.dto.UpdateTransactionRequest;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/finance/transactions")
@Validated
@Tag(name = "Finances", description = "Opérations financières d'un contexte")
public class FinanceTransactionController {

    private final ListTransactionsUseCase listTransactionsUseCase;
    private final CreateTransactionUseCase createTransactionUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;
    private final MoveTransactionUseCase moveTransactionUseCase;

    public FinanceTransactionController(ListTransactionsUseCase listTransactionsUseCase, CreateTransactionUseCase createTransactionUseCase,
                                         UpdateTransactionUseCase updateTransactionUseCase, DeleteTransactionUseCase deleteTransactionUseCase,
                                         MoveTransactionUseCase moveTransactionUseCase) {
        this.listTransactionsUseCase = listTransactionsUseCase;
        this.createTransactionUseCase = createTransactionUseCase;
        this.updateTransactionUseCase = updateTransactionUseCase;
        this.deleteTransactionUseCase = deleteTransactionUseCase;
        this.moveTransactionUseCase = moveTransactionUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TransactionResponse>> list(
            @PathVariable UUID spaceId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listTransactionsUseCase.list(month, membership).stream().map(TransactionResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransactionResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody CreateTransactionRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Transaction created = createTransactionUseCase.create(new CreateTransactionCommand(
            spaceId, request.label(), request.amount(), request.type(), request.categoryId(), request.date(),
            request.payerId(), toContributionInputs(request.contributors()), null), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(created));
    }

    @PatchMapping("/{transactionId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID transactionId, @Valid @RequestBody UpdateTransactionRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Transaction updated = updateTransactionUseCase.update(new UpdateTransactionCommand(
            transactionId, spaceId, request.label(), request.amount(), request.type(), request.categoryId(),
            request.date(), request.payerId(), toContributionInputs(request.contributors())), membership);
        return ResponseEntity.ok(TransactionResponse.from(updated));
    }

    @DeleteMapping("/{transactionId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID transactionId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteTransactionUseCase.delete(transactionId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{transactionId}/move")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransactionResponse> move(
            @PathVariable UUID spaceId, @PathVariable UUID transactionId, @Valid @RequestBody MoveTransactionRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Transaction moved = moveTransactionUseCase.move(transactionId, request.destinationSpaceId(), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(moved));
    }

    static List<ContributionInput> toContributionInputs(List<ContributionRequest> requests) {
        return requests.stream().map(r -> new ContributionInput(r.memberId(), r.shareAmount())).toList();
    }
}
