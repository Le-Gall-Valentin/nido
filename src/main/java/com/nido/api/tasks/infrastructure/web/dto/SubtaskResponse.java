package com.nido.api.tasks.infrastructure.web.dto;

import com.nido.api.tasks.domain.model.Subtask;

import java.util.UUID;

public record SubtaskResponse(UUID id, String text, boolean done) {
    public static SubtaskResponse from(Subtask s) {
        return new SubtaskResponse(s.id(), s.text(), s.done());
    }
}
