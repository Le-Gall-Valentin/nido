package com.nido.api.shopping.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ShoppingCategory(UUID id, UUID spaceId, String name, int position, boolean fallback) {
    public ShoppingCategory {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(name, "name");
    }
}
