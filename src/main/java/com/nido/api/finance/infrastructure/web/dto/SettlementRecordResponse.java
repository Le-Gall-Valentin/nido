package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.SettlementRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SettlementRecordResponse(UUID id, UUID fromMemberId, UUID toMemberId, BigDecimal amount, LocalDate date) {
    public static SettlementRecordResponse from(SettlementRecord s) {
        return new SettlementRecordResponse(s.id(), s.fromMemberId(), s.toMemberId(), s.amount(), s.date());
    }
}
