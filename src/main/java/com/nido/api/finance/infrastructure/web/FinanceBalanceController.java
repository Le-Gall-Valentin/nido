package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.application.port.in.GetBalancesUseCase;
import com.nido.api.finance.application.port.in.ListSettlementsBetweenMembersUseCase;
import com.nido.api.finance.application.port.in.SettleDebtUseCase;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.infrastructure.web.dto.BalancesResponse;
import com.nido.api.finance.infrastructure.web.dto.SettleDebtRequest;
import com.nido.api.finance.infrastructure.web.dto.SettlementRecordResponse;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/finance/balances")
@Validated
@Tag(name = "Finances", description = "Remboursements entre membres d'un contexte")
public class FinanceBalanceController {

    private final GetBalancesUseCase getBalancesUseCase;
    private final SettleDebtUseCase settleDebtUseCase;
    private final ListSettlementsBetweenMembersUseCase listSettlementsBetweenMembersUseCase;

    public FinanceBalanceController(
            GetBalancesUseCase getBalancesUseCase, SettleDebtUseCase settleDebtUseCase,
            ListSettlementsBetweenMembersUseCase listSettlementsBetweenMembersUseCase) {
        this.getBalancesUseCase = getBalancesUseCase;
        this.settleDebtUseCase = settleDebtUseCase;
        this.listSettlementsBetweenMembersUseCase = listSettlementsBetweenMembersUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BalancesResponse> get(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(BalancesResponse.from(getBalancesUseCase.getBalances(membership)));
    }

    @GetMapping("/settlements")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SettlementRecordResponse>> listSettlements(
            @PathVariable UUID spaceId, @RequestParam UUID memberAId, @RequestParam UUID memberBId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listSettlementsBetweenMembersUseCase.list(memberAId, memberBId, membership).stream()
            .map(SettlementRecordResponse::from).toList());
    }

    @PostMapping("/settle")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SettlementRecordResponse> settle(
            @PathVariable UUID spaceId, @Valid @RequestBody SettleDebtRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        SettlementRecord created = settleDebtUseCase.settle(
            new CreateSettlementCommand(spaceId, request.fromMemberId(), request.toMemberId(), request.amount(), request.date()), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(SettlementRecordResponse.from(created));
    }
}
