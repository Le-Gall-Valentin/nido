package com.nido.api.kitchen.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.kitchen.application.port.in.CreateRecipeUseCase;
import com.nido.api.kitchen.application.port.in.DeleteRecipeUseCase;
import com.nido.api.kitchen.application.port.in.GetRecipeUseCase;
import com.nido.api.kitchen.application.port.in.ListRecipesUseCase;
import com.nido.api.kitchen.application.port.in.ToggleRecipeFavoriteUseCase;
import com.nido.api.kitchen.application.port.in.UpdateRecipeUseCase;
import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.model.UpdateRecipeCommand;
import com.nido.api.kitchen.infrastructure.web.dto.CreateRecipeRequest;
import com.nido.api.kitchen.infrastructure.web.dto.RecipeIngredientRequest;
import com.nido.api.kitchen.infrastructure.web.dto.RecipeResponse;
import com.nido.api.kitchen.infrastructure.web.dto.RecipeSummaryResponse;
import com.nido.api.kitchen.infrastructure.web.dto.UpdateRecipeRequest;
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
@RequestMapping("/api/spaces/{spaceId}/kitchen/recipes")
@Validated
@Tag(name = "Recettes", description = "Recettes de cuisine d'un contexte")
public class RecipeController {

    private final ListRecipesUseCase listRecipesUseCase;
    private final GetRecipeUseCase getRecipeUseCase;
    private final CreateRecipeUseCase createRecipeUseCase;
    private final UpdateRecipeUseCase updateRecipeUseCase;
    private final DeleteRecipeUseCase deleteRecipeUseCase;
    private final ToggleRecipeFavoriteUseCase toggleRecipeFavoriteUseCase;

    public RecipeController(ListRecipesUseCase listRecipesUseCase,
                            GetRecipeUseCase getRecipeUseCase,
                            CreateRecipeUseCase createRecipeUseCase,
                            UpdateRecipeUseCase updateRecipeUseCase,
                            DeleteRecipeUseCase deleteRecipeUseCase,
                            ToggleRecipeFavoriteUseCase toggleRecipeFavoriteUseCase) {
        this.listRecipesUseCase = listRecipesUseCase;
        this.getRecipeUseCase = getRecipeUseCase;
        this.createRecipeUseCase = createRecipeUseCase;
        this.updateRecipeUseCase = updateRecipeUseCase;
        this.deleteRecipeUseCase = deleteRecipeUseCase;
        this.toggleRecipeFavoriteUseCase = toggleRecipeFavoriteUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecipeSummaryResponse>> list(
            @PathVariable UUID spaceId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listRecipesUseCase.list(membership).stream()
            .map(RecipeSummaryResponse::from).toList());
    }

    @GetMapping("/{recipeId}")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecipeResponse> get(
            @PathVariable UUID spaceId, @PathVariable UUID recipeId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(RecipeResponse.from(getRecipeUseCase.get(recipeId, membership)));
    }

    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecipeResponse> create(
            @PathVariable UUID spaceId,
            @Valid @RequestBody CreateRecipeRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        CreateRecipeCommand command = new CreateRecipeCommand(spaceId, request.name(), request.description(), request.category(),
            request.minutes(), request.referencePortions(), toDomainIngredients(request.ingredients()), request.steps(), request.note());
        Recipe created = createRecipeUseCase.create(command, membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(RecipeResponse.from(created));
    }

    @PatchMapping("/{recipeId}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecipeResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID recipeId,
            @Valid @RequestBody UpdateRecipeRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        UpdateRecipeCommand command = new UpdateRecipeCommand(recipeId, spaceId, request.name(), request.description(), request.category(),
            request.minutes(), request.referencePortions(), toDomainIngredients(request.ingredients()), request.steps(), request.note());
        return ResponseEntity.ok(RecipeResponse.from(updateRecipeUseCase.update(command, membership)));
    }

    @DeleteMapping("/{recipeId}")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID recipeId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteRecipeUseCase.delete(recipeId, membership);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{recipeId}/favorite")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecipeResponse> toggleFavorite(
            @PathVariable UUID spaceId, @PathVariable UUID recipeId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(RecipeResponse.from(toggleRecipeFavoriteUseCase.toggleFavorite(recipeId, membership)));
    }

    private static List<RecipeIngredient> toDomainIngredients(List<RecipeIngredientRequest> requested) {
        return requested.stream()
            .map(i -> new RecipeIngredient(i.name(), i.quantity(), i.unit()))
            .toList();
    }
}
