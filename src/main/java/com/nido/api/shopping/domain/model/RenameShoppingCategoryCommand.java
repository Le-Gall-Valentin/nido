package com.nido.api.shopping.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RenameShoppingCategoryCommand(UUID categoryId, UUID spaceId, String name) {
    public RenameShoppingCategoryCommand {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(name, "name");
    }
}
