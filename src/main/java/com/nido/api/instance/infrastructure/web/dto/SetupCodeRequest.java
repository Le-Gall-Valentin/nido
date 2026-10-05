package com.nido.api.instance.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SetupCodeRequest(@NotBlank String code) {
    @Override public String toString() { return "SetupCodeRequest[code=***]"; }
}
