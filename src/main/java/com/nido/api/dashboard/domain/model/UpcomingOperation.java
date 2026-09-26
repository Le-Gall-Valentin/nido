package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** {@code type} is the finance module's transaction type name: EXPENSE or INCOME. */
public record UpcomingOperation(LocalDate date, String label, BigDecimal amount, String type, UUID seriesId) {
}
