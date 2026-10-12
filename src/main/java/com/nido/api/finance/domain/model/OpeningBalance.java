package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * The balance of a space's account at the end of a day, that day's operations included — what a bank statement shows,
 * and what every balance of the space is computed from. May be negative: an overdraft is a balance too.
 */
public record OpeningBalance(UUID spaceId, BigDecimal amount, LocalDate asOf) {
    public OpeningBalance {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(asOf, "asOf");
    }
}
