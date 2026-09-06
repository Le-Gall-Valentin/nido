package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.SavingsGoalDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SavingsGoalResponse(
    UUID id, String name, BigDecimal targetAmount, LocalDate targetDate, String color, String glyph, BigDecimal totalContributed,
    List<SavingsContributionResponse> contributions
) {
    public static SavingsGoalResponse from(SavingsGoalDetail detail) {
        BigDecimal total = detail.contributions().stream()
            .map(com.nido.api.finance.domain.model.SavingsContribution::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SavingsGoalResponse(detail.goal().id(), detail.goal().name(), detail.goal().targetAmount(),
            detail.goal().targetDate(), detail.goal().color(), detail.goal().glyph(), total,
            detail.contributions().stream().map(SavingsContributionResponse::from).toList());
    }
}
