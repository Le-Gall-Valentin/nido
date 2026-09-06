package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.SavingsContribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SavingsContributionResponse(UUID id, UUID memberId, BigDecimal amount, LocalDate date) {
    public static SavingsContributionResponse from(SavingsContribution c) {
        return new SavingsContributionResponse(c.id(), c.memberId(), c.amount(), c.date());
    }
}
