package com.nido.api.shopping.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.shopping.application.port.in.AddShoppingItemUseCase;
import com.nido.api.shopping.application.port.in.ClearAllShoppingItemsUseCase;
import com.nido.api.shopping.application.port.in.ClearDoneShoppingItemsUseCase;
import com.nido.api.shopping.application.port.in.DeleteShoppingItemUseCase;
import com.nido.api.shopping.application.port.in.ImportShoppingItemsFromMenuUseCase;
import com.nido.api.shopping.application.port.in.ListShoppingItemsUseCase;
import com.nido.api.shopping.application.port.in.ToggleShoppingItemDoneUseCase;
import com.nido.api.shopping.application.port.in.UpdateShoppingItemUseCase;
import com.nido.api.shopping.domain.model.AddShoppingItemCommand;
import com.nido.api.shopping.domain.model.ImportShoppingItemsCommand;
import com.nido.api.shopping.domain.model.ShoppingImportLine;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;
import com.nido.api.shopping.infrastructure.web.dto.AddShoppingItemRequest;
import com.nido.api.shopping.infrastructure.web.dto.ImportShoppingItemsRequest;
import com.nido.api.shopping.infrastructure.web.dto.ShoppingItemResponse;
import com.nido.api.shopping.infrastructure.web.dto.UpdateShoppingItemRequest;
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
@RequestMapping("/api/spaces/{spaceId}/shopping/items")
@Validated
@Tag(name = "Courses — articles", description = "Articles de la liste de courses d'un contexte")
public class ShoppingItemController {

    private final ListShoppingItemsUseCase listShoppingItemsUseCase;
    private final AddShoppingItemUseCase addShoppingItemUseCase;
    private final UpdateShoppingItemUseCase updateShoppingItemUseCase;
    private final ToggleShoppingItemDoneUseCase toggleShoppingItemDoneUseCase;
    private final DeleteShoppingItemUseCase deleteShoppingItemUseCase;
    private final ClearDoneShoppingItemsUseCase clearDoneShoppingItemsUseCase;
    private final ClearAllShoppingItemsUseCase clearAllShoppingItemsUseCase;
    private final ImportShoppingItemsFromMenuUseCase importShoppingItemsFromMenuUseCase;

    public ShoppingItemController(ListShoppingItemsUseCase listShoppingItemsUseCase,
                                  AddShoppingItemUseCase addShoppingItemUseCase,
                                  UpdateShoppingItemUseCase updateShoppingItemUseCase,
                                  ToggleShoppingItemDoneUseCase toggleShoppingItemDoneUseCase,
                                  DeleteShoppingItemUseCase deleteShoppingItemUseCase,
                                  ClearDoneShoppingItemsUseCase clearDoneShoppingItemsUseCase,
                                  ClearAllShoppingItemsUseCase clearAllShoppingItemsUseCase,
                                  ImportShoppingItemsFromMenuUseCase importShoppingItemsFromMenuUseCase) {
        this.listShoppingItemsUseCase = listShoppingItemsUseCase;
        this.addShoppingItemUseCase = addShoppingItemUseCase;
        this.updateShoppingItemUseCase = updateShoppingItemUseCase;
        this.toggleShoppingItemDoneUseCase = toggleShoppingItemDoneUseCase;
        this.deleteShoppingItemUseCase = deleteShoppingItemUseCase;
        this.clearDoneShoppingItemsUseCase = clearDoneShoppingItemsUseCase;
        this.clearAllShoppingItemsUseCase = clearAllShoppingItemsUseCase;
        this.importShoppingItemsFromMenuUseCase = importShoppingItemsFromMenuUseCase;
    }

    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ShoppingItemResponse>> list(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        return ResponseEntity.ok(listShoppingItemsUseCase.list(membership).stream().map(ShoppingItemResponse::from).toList());
    }

    @PostMapping
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShoppingItemResponse> add(
            @PathVariable UUID spaceId, @Valid @RequestBody AddShoppingItemRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        ShoppingItem created = addShoppingItemUseCase.add(
            new AddShoppingItemCommand(spaceId, request.categoryId(), request.name(), request.quantityLabel()), membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShoppingItemResponse.from(created));
    }

    @PatchMapping("/{itemId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShoppingItemResponse> update(
            @PathVariable UUID spaceId, @PathVariable UUID itemId, @Valid @RequestBody UpdateShoppingItemRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        ShoppingItem updated = updateShoppingItemUseCase.update(
            new UpdateShoppingItemCommand(itemId, spaceId, request.categoryId(), request.name(), request.quantityLabel()), membership);
        return ResponseEntity.ok(ShoppingItemResponse.from(updated));
    }

    @PatchMapping("/{itemId}/done")
    @RateLimiting(max = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleDone(
            @PathVariable UUID spaceId, @PathVariable UUID itemId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        toggleShoppingItemDoneUseCase.toggle(itemId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{itemId}")
    @RateLimiting(max = 40)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID spaceId, @PathVariable UUID itemId,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        deleteShoppingItemUseCase.delete(itemId, spaceId, membership);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/clear-done")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearDone(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        clearDoneShoppingItemsUseCase.clearDone(spaceId, membership);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/clear-all")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearAll(
            @PathVariable UUID spaceId, @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        clearAllShoppingItemsUseCase.clearAll(spaceId, membership);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import-from-menu")
    @RateLimiting(max = 20)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ShoppingItemResponse>> importFromMenu(
            @PathVariable UUID spaceId, @Valid @RequestBody ImportShoppingItemsRequest request,
            @Parameter(hidden = true) @CurrentMembership SpaceMembership membership) {
        List<ShoppingImportLine> lines = request.lines().stream()
            .map(l -> new ShoppingImportLine(l.name(), l.quantityLabel(), l.categoryId())).toList();
        List<ShoppingItem> imported = importShoppingItemsFromMenuUseCase.importItems(
            new ImportShoppingItemsCommand(spaceId, lines), membership);
        return ResponseEntity.ok(imported.stream().map(ShoppingItemResponse::from).toList());
    }
}
