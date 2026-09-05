package com.nido.api.finance.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CreateCategoryCommand(UUID spaceId, String label, String color, String icon) {
    public CreateCategoryCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(icon, "icon");
    }
}
