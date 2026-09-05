package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.Contribution;

import java.math.BigDecimal;
import java.util.UUID;

public record ContributionResponse(UUID memberId, BigDecimal shareAmount) {
    public static ContributionResponse from(Contribution c) {
        return new ContributionResponse(c.memberId(), c.shareAmount());
    }
}
