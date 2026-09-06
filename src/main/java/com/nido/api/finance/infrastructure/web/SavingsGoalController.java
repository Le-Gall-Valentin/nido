package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.application.port.in.AddSavingsContributionUseCase;
import com.nido.api.finance.application.port.in.CreateSavingsGoalUseCase;
import com.nido.api.finance.application.port.in.DeleteSavingsGoalUseCase;
import com.nido.api.finance.application.port.in.ListSavingsGoalsUseCase;
import com.nido.api.finance.application.port.in.UpdateSavingsGoalUseCase;
import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
import com.nido.api.finance.infrastructure.web.dto.AddSavingsContributionRequest;
import com.nido.api.finance.infrastructure.web.dto.CreateSavingsGoalRequest;
import com.nido.api.finance.infrastructure.web.dto.SavingsContributionResponse;
import com.nido.api.finance.infrastructure.web.dto.SavingsGoalResponse;
import com.nido.api.finance.infrastructure.web.dto.UpdateSavingsGoalRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/finance/savings-goals")
@Validated
@Tag(name = "Finances", description = "Objectifs d'épargne partagés d'un contexte")
public class SavingsGoalController {

    private final ListSavingsGoalsUseCase listSavingsGoalsUseCase;
    private final CreateSavingsGoalUseCase createSavingsGoalUseCase;
    private final UpdateSavingsGoalUseCase updateSavingsGoalUseCase;
    private final DeleteSavingsGoalUseCase deleteSavingsGoalUseCase;
    private final AddSavingsContributionUseCase addSavingsContributionUseCase;

    public SavingsGoalController(ListSavingsGoalsUseCase listSavingsGoalsUseCase, CreateSavingsGoalUseCase createSavingsGoalUseCase,
                                  UpdateSavingsGoalUseCase updateSavingsGoalUseCase, DeleteSavingsGoalUseCase deleteSavingsGoalUseCase,
                                  AddSavingsContributionUseCase addSavingsContributionUseCase) {
        this.listSavingsGoalsUseCase = listSavingsGoalsUseCase;
        this.createSavingsGoalUseCase = createSavingsGoalUseCase;
        this.updateSavingsGoalUseCase = updateSavingsGoalUseCase;
        this.deleteSavingsGoalUseCase = deleteSavingsGoalUseCase;
        this.addSavingsContributionUseCase = addSavingsContributionUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SavingsGoalResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listSavingsGoalsUseCase.list(membership).stream().map(SavingsGoalResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SavingsGoalResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody CreateSavingsGoalRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        SavingsGoal created = createSavingsGoalUseCase.create(
            new CreateSavingsGoalCommand(spaceId, request.name(), request.targetAmount(), request.targetDate(), request.color(), request.glyph()), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(SavingsGoalResponse.from(new SavingsGoalDetail(created, List.of())));
    }

    @PatchMapping("/{goalId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SavingsGoalResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID goalId, @Valid @RequestBody UpdateSavingsGoalRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(SavingsGoalResponse.from(updateSavingsGoalUseCase.update(
            new UpdateSavingsGoalCommand(goalId, spaceId, request.name(), request.targetAmount(), request.targetDate(), request.color(), request.glyph()), membership)));
    }

    @DeleteMapping("/{goalId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID goalId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteSavingsGoalUseCase.delete(goalId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{goalId}/contributions")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SavingsContributionResponse> addContribution(
            @PathVariable UUID spaceId, @PathVariable UUID goalId, @Valid @RequestBody AddSavingsContributionRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        SavingsContribution created = addSavingsContributionUseCase.add(
            new AddSavingsContributionCommand(goalId, spaceId, request.memberId(), request.amount(), request.date()), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(SavingsContributionResponse.from(created));
    }
}
