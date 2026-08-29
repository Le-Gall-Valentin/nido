package com.nido.api.kitchen.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.kitchen.application.port.in.AddMenuEntryUseCase;
import com.nido.api.kitchen.application.port.in.ComputeShoppingListUseCase;
import com.nido.api.kitchen.application.port.in.ListMenuEntriesUseCase;
import com.nido.api.kitchen.application.port.in.RemoveMenuEntryUseCase;
import com.nido.api.kitchen.application.port.in.UpdateMenuEntryPortionsUseCase;
import com.nido.api.kitchen.domain.model.AddMenuEntryCommand;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.UpdateMenuEntryPortionsCommand;
import com.nido.api.kitchen.infrastructure.web.dto.AddMenuEntryRequest;
import com.nido.api.kitchen.infrastructure.web.dto.MenuEntryResponse;
import com.nido.api.kitchen.infrastructure.web.dto.ShoppingListLineResponse;
import com.nido.api.kitchen.infrastructure.web.dto.UpdateMenuEntryPortionsRequest;
import com.nido.api.space.domain.model.SpaceMembership;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces/{spaceId}/kitchen/menu")
@Validated
@Tag(name = "Menu", description = "Planification hebdomadaire des repas et liste de courses")
public class MenuController {

    private final ListMenuEntriesUseCase listMenuEntriesUseCase;
    private final AddMenuEntryUseCase addMenuEntryUseCase;
    private final UpdateMenuEntryPortionsUseCase updateMenuEntryPortionsUseCase;
    private final RemoveMenuEntryUseCase removeMenuEntryUseCase;
    private final ComputeShoppingListUseCase computeShoppingListUseCase;

    public MenuController(ListMenuEntriesUseCase listMenuEntriesUseCase,
                          AddMenuEntryUseCase addMenuEntryUseCase,
                          UpdateMenuEntryPortionsUseCase updateMenuEntryPortionsUseCase,
                          RemoveMenuEntryUseCase removeMenuEntryUseCase,
                          ComputeShoppingListUseCase computeShoppingListUseCase) {
        this.listMenuEntriesUseCase = listMenuEntriesUseCase;
        this.addMenuEntryUseCase = addMenuEntryUseCase;
        this.updateMenuEntryPortionsUseCase = updateMenuEntryPortionsUseCase;
        this.removeMenuEntryUseCase = removeMenuEntryUseCase;
        this.computeShoppingListUseCase = computeShoppingListUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MenuEntryResponse>> list(
            @PathVariable UUID spaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listMenuEntriesUseCase.list(membership, from, to).stream()
            .map(MenuEntryResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MenuEntryResponse> add(
            @PathVariable UUID spaceId,
            @Valid @RequestBody AddMenuEntryRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        MenuEntryView created = addMenuEntryUseCase.add(
            new AddMenuEntryCommand(spaceId, request.date(), request.recipeId(), request.portions()), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(MenuEntryResponse.from(created));
    }

    @PatchMapping("/{entryId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updatePortions(
            @PathVariable UUID spaceId, @PathVariable UUID entryId,
            @Valid @RequestBody UpdateMenuEntryPortionsRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        updateMenuEntryPortionsUseCase.updatePortions(
            new UpdateMenuEntryPortionsCommand(entryId, spaceId, request.portions()), membership);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{entryId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> remove(
            @PathVariable UUID spaceId, @PathVariable UUID entryId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        removeMenuEntryUseCase.remove(entryId, membership);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shopping-list")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ShoppingListLineResponse>> shoppingList(
            @PathVariable UUID spaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(computeShoppingListUseCase.compute(membership, from, to).stream()
            .map(ShoppingListLineResponse::from).toList());
    }
}
