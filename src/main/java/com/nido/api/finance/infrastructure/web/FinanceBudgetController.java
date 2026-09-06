package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.application.port.in.DeleteBudgetUseCase;
import com.nido.api.finance.application.port.in.ListBudgetsUseCase;
import com.nido.api.finance.application.port.in.SetBudgetUseCase;
import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.SetBudgetCommand;
import com.nido.api.finance.infrastructure.web.dto.BudgetResponse;
import com.nido.api.finance.infrastructure.web.dto.SetBudgetRequest;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/finance/budgets")
@Validated
@Tag(name = "Finances", description = "Budgets par catégorie d'un contexte")
public class FinanceBudgetController {

    private final ListBudgetsUseCase listBudgetsUseCase;
    private final SetBudgetUseCase setBudgetUseCase;
    private final DeleteBudgetUseCase deleteBudgetUseCase;

    public FinanceBudgetController(
            ListBudgetsUseCase listBudgetsUseCase, SetBudgetUseCase setBudgetUseCase, DeleteBudgetUseCase deleteBudgetUseCase) {
        this.listBudgetsUseCase = listBudgetsUseCase;
        this.setBudgetUseCase = setBudgetUseCase;
        this.deleteBudgetUseCase = deleteBudgetUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BudgetResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listBudgetsUseCase.list(membership).stream().map(BudgetResponse::from).toList());
    }

    @PutMapping("/{categoryId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BudgetResponse> set(
            @PathVariable UUID spaceId, @PathVariable UUID categoryId, @Valid @RequestBody SetBudgetRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Budget saved = setBudgetUseCase.set(new SetBudgetCommand(spaceId, categoryId, request.monthlyLimit()), membership);
        return ResponseEntity.ok(BudgetResponse.from(saved));
    }

    @DeleteMapping("/{categoryId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID categoryId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteBudgetUseCase.delete(spaceId, categoryId, membership);
        return ResponseEntity.noContent().build();
    }
}
