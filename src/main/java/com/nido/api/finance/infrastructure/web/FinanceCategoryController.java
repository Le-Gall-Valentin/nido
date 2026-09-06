package com.nido.api.finance.infrastructure.web;

import com.nido.api.finance.application.port.in.CreateCategoryUseCase;
import com.nido.api.finance.application.port.in.DeleteCategoryUseCase;
import com.nido.api.finance.application.port.in.ListCategoriesUseCase;
import com.nido.api.finance.application.port.in.UpdateCategoryUseCase;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.UpdateCategoryCommand;
import com.nido.api.finance.infrastructure.web.dto.CategoryResponse;
import com.nido.api.finance.infrastructure.web.dto.CreateCategoryRequest;
import com.nido.api.finance.infrastructure.web.dto.UpdateCategoryRequest;
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
@RequestMapping("/api/spaces/{spaceId}/finance/categories")
@Validated
@Tag(name = "Finances", description = "Catégories de Finances d'un contexte")
public class FinanceCategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public FinanceCategoryController(ListCategoriesUseCase listCategoriesUseCase, CreateCategoryUseCase createCategoryUseCase,
                                      UpdateCategoryUseCase updateCategoryUseCase, DeleteCategoryUseCase deleteCategoryUseCase) {
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoryResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listCategoriesUseCase.list(membership).stream().map(CategoryResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody CreateCategoryRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Category created = createCategoryUseCase.create(
            new CreateCategoryCommand(spaceId, request.label(), request.color(), request.icon(), request.type()), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(created));
    }

    @PatchMapping("/{categoryId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID categoryId, @Valid @RequestBody UpdateCategoryRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        Category updated = updateCategoryUseCase.update(
            new UpdateCategoryCommand(categoryId, spaceId, request.label(), request.color(), request.icon()), membership);
        return ResponseEntity.ok(CategoryResponse.from(updated));
    }

    @DeleteMapping("/{categoryId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID categoryId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteCategoryUseCase.delete(categoryId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }
}
