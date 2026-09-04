package com.nido.api.tasks.domain.model;

import java.util.Objects;

/**
 * A subtask to persist on creation — distinct from {@link Subtask} (which
 * always has a real, already-assigned id) because the three creation paths
 * disagree on the starting {@code done} value: a fresh one-off task and a
 * freshly generated recurring occurrence always start every subtask
 * unchecked, while moving a task to another context must preserve each
 * subtask's current done state exactly. Carrying that flag here — instead
 * of always defaulting to false inside the repository — is what lets
 * {@code MoveTaskHandler} reuse the exact same {@code TaskRepository.create}
 * entry point as task creation.
 */
public record SubtaskInput(String text, boolean done) {
    public SubtaskInput {
        Objects.requireNonNull(text, "text");
    }
}
