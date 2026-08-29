package com.nido.api.kitchen.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record AddMenuEntryCommand(UUID spaceId, LocalDate date, UUID recipeId, int portions) {
    public AddMenuEntryCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(recipeId, "recipeId");
    }
}
