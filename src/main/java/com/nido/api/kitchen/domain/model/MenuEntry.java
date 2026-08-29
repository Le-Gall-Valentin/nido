package com.nido.api.kitchen.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record MenuEntry(UUID id, UUID spaceId, LocalDate date, UUID recipeId, int portions, int position) {
    public MenuEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(recipeId, "recipeId");
    }
}
