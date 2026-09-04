package com.nido.api.tasks.domain.model;

import java.util.Objects;
import java.util.UUID;

public record Subtask(UUID id, String text, boolean done) {
    public Subtask {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(text, "text");
    }
}
