package com.nido.api.tasks.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * One line of a task's subtask list as an edit sends it back — distinct from {@link Subtask} and
 * {@link SubtaskInput} because it carries no {@code done} flag at all: the check belongs to the
 * subtask, not to the form that renames it. An {@code id} names a subtask the task already has,
 * which keeps its check; a null {@code id} is a new one, which starts unchecked.
 */
public record SubtaskEdit(UUID id, String text) {
    public SubtaskEdit {
        Objects.requireNonNull(text, "text");
    }
}
