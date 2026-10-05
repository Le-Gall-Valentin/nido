package com.nido.api.instance.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Valeurs saisies dans un bloc de la page de réglages, par code de réglage (ex. `mail.host`)")
public record SettingsValuesRequest(@NotNull Map<String, String> values) {
    @Override
    public String toString() {
        return "SettingsValuesRequest[keys=" + (values == null ? "[]" : values.keySet()) + "]";
    }
}
