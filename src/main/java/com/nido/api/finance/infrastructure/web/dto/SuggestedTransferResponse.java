package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.SuggestedTransfer;

import java.math.BigDecimal;
import java.util.UUID;

public record SuggestedTransferResponse(UUID fromMemberId, UUID toMemberId, BigDecimal amount) {
    public static SuggestedTransferResponse from(SuggestedTransfer t) {
        return new SuggestedTransferResponse(t.fromMemberId(), t.toMemberId(), t.amount());
    }
}
