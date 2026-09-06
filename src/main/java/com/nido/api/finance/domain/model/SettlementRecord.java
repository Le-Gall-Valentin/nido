package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record SettlementRecord(UUID id, UUID spaceId, UUID fromMemberId, UUID toMemberId, BigDecimal amount, LocalDate date) {
    public SettlementRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(fromMemberId, "fromMemberId");
        Objects.requireNonNull(toMemberId, "toMemberId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(date, "date");
    }
}
