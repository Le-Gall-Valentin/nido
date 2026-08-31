package com.nido.api.shopping.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AddShoppingItemCommand(UUID spaceId, UUID categoryId, String name, String quantityLabel) {
    public AddShoppingItemCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(name, "name");
    }
}
