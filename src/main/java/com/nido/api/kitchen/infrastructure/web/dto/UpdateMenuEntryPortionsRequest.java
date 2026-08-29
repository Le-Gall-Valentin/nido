package com.nido.api.kitchen.infrastructure.web.dto;

import jakarta.validation.constraints.Min;

public record UpdateMenuEntryPortionsRequest(@Min(1) int portions) {}
