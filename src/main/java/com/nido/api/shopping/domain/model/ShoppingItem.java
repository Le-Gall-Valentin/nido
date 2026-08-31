package com.nido.api.shopping.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ShoppingItem(UUID id, UUID spaceId, UUID categoryId, String name, String quantityLabel, boolean done, int position) {
    public ShoppingItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(name, "name");
    }
}
