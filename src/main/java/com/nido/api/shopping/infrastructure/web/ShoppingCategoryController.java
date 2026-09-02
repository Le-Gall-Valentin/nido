package com.nido.api.shopping.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.shopping.application.port.in.CreateShoppingCategoryUseCase;
import com.nido.api.shopping.application.port.in.DeleteShoppingCategoryUseCase;
import com.nido.api.shopping.application.port.in.ListShoppingCategoriesUseCase;
import com.nido.api.shopping.application.port.in.RenameShoppingCategoryUseCase;
import com.nido.api.shopping.domain.model.RenameShoppingCategoryCommand;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.infrastructure.web.dto.CreateShoppingCategoryRequest;
import com.nido.api.shopping.infrastructure.web.dto.RenameShoppingCategoryRequest;
import com.nido.api.shopping.infrastructure.web.dto.ShoppingCategoryResponse;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/shopping/categories")
@Validated
@Tag(name = "Courses — catégories", description = "Catégories de la liste de courses d'un contexte")
public class ShoppingCategoryController {

    private final ListShoppingCategoriesUseCase listShoppingCategoriesUseCase;
    private final CreateShoppingCategoryUseCase createShoppingCategoryUseCase;
    private final RenameShoppingCategoryUseCase renameShoppingCategoryUseCase;
    private final DeleteShoppingCategoryUseCase deleteShoppingCategoryUseCase;

    public ShoppingCategoryController(ListShoppingCategoriesUseCase listShoppingCategoriesUseCase,
                                      CreateShoppingCategoryUseCase createShoppingCategoryUseCase,
                                      RenameShoppingCategoryUseCase renameShoppingCategoryUseCase,
                                      DeleteShoppingCategoryUseCase deleteShoppingCategoryUseCase) {
        this.listShoppingCategoriesUseCase = listShoppingCategoriesUseCase;
        this.createShoppingCategoryUseCase = createShoppingCategoryUseCase;
        this.renameShoppingCategoryUseCase = renameShoppingCategoryUseCase;
        this.deleteShoppingCategoryUseCase = deleteShoppingCategoryUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ShoppingCategoryResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listShoppingCategoriesUseCase.list(membership).stream()
            .map(ShoppingCategoryResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShoppingCategoryResponse> create(
            @PathVariable UUID spaceId, @Valid @RequestBody CreateShoppingCategoryRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        ShoppingCategory created = createShoppingCategoryUseCase.create(spaceId, request.name(), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShoppingCategoryResponse.from(created));
    }

    @PatchMapping("/{categoryId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShoppingCategoryResponse> rename(
            @PathVariable UUID spaceId, @PathVariable UUID categoryId, @Valid @RequestBody RenameShoppingCategoryRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        ShoppingCategory renamed = renameShoppingCategoryUseCase.rename(
            new RenameShoppingCategoryCommand(categoryId, spaceId, request.name()), membership);
        return ResponseEntity.ok(ShoppingCategoryResponse.from(renamed));
    }

    @DeleteMapping("/{categoryId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID categoryId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteShoppingCategoryUseCase.delete(categoryId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }
}
