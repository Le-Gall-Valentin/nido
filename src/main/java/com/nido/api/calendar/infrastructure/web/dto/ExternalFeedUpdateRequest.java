package com.nido.api.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExternalFeedUpdateRequest(@NotBlank @Size(max = 100) String name, @Size(max = 30) String color) {}
