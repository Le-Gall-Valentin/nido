package com.nido.api.finance.domain.model;

import java.util.Objects;
import java.util.UUID;

public record Category(UUID id, UUID spaceId, String label, String color, String icon, boolean isDefault, TransactionType type) {
    public Category {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(type, "type");
    }
}
