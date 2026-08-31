package com.nido.api.shopping.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UpdateShoppingItemCommand(UUID itemId, UUID spaceId, UUID categoryId, String name, String quantityLabel) {
    public UpdateShoppingItemCommand {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(name, "name");
    }
}
