package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.MemberBalance;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberBalanceResponse(UUID memberId, BigDecimal net) {
    public static MemberBalanceResponse from(MemberBalance m) {
        return new MemberBalanceResponse(m.memberId(), m.net());
    }
}
