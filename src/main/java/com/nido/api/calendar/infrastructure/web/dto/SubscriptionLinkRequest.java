package com.nido.api.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubscriptionLinkRequest(
    @NotBlank @Size(max = 60) String label,
    // Names rather than the enum, as the export takes them: a name nido does not know is answered invalid_sources.
    @Size(max = 5) List<@NotNull String> sources,
    Boolean onlyMine
) {
    public boolean onlyMineOrDefault() {
        return Boolean.TRUE.equals(onlyMine);
    }
}
