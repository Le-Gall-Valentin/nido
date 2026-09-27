package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** A budget worth a look: one at 80 % of its limit or past it — a budget within its limit has no line. */
public record BudgetWatch(UUID categoryId, String label, String color, BigDecimal spent, BigDecimal limit, Status status) {

    /** Spelled as the finance module spells its budget statuses; the client reads those names. */
    public enum Status { WARNING, OVER }
}
