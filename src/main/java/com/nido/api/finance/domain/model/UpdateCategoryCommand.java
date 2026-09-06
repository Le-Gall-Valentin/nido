package com.nido.api.finance.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UpdateCategoryCommand(UUID categoryId, UUID spaceId, String label, String color, String icon) {
    public UpdateCategoryCommand {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(icon, "icon");
    }
}
