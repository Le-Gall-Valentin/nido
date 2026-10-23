package com.nido.api.calendar.domain.model;

import java.time.LocalDate;

/** An occurrence of an imported series edited on its own: the slot it takes over, and what it became. */
public record ImportedOccurrence(LocalDate slot, ImportedEvent event) {}
