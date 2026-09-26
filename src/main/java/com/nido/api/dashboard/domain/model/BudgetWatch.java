package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code status} is the finance module's budget status name: WARNING or OVER. */
public record BudgetWatch(UUID categoryId, String label, String color, BigDecimal spent, BigDecimal limit, String status) {
}
