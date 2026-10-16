package com.nido.api.calendar.infrastructure.web.dto;

import com.nido.api.calendar.domain.model.CalendarSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubscriptionLinkRequest(
    @NotBlank @Size(max = 60) String label,
    @Size(max = 5) List<@NotNull CalendarSourceType> sources,
    Boolean onlyMine
) {
    public boolean onlyMineOrDefault() {
        return Boolean.TRUE.equals(onlyMine);
    }
}
