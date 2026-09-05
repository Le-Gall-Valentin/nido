package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** A contributor's resolved, persisted share of a transaction or series — always a fixed amount, never null. */
public record Contribution(UUID memberId, BigDecimal shareAmount) {
    public Contribution {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(shareAmount, "shareAmount");
    }
}
