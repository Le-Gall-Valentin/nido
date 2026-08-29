package com.nido.api.kitchen.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.UUID;

/** JPQL constructor-expression projection for the "when was this recipe last planned" query. */
public record LastPlannedOn(UUID recipeId, LocalDate lastDate) {}
