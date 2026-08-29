package com.nido.api.kitchen.domain.model;

import java.time.LocalDate;

public record RecipeSummaryView(Recipe recipe, LocalDate lastPlannedOn) {}
