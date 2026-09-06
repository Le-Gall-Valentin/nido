package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.Projection;

import java.math.BigDecimal;
import java.util.List;

public record ProjectionResponse(BigDecimal actualBalanceSoFar, List<ProjectedOccurrenceResponse> upcoming, BigDecimal projectedEndOfMonthBalance) {
    public static ProjectionResponse from(Projection p) {
        return new ProjectionResponse(p.actualBalanceSoFar(), p.upcoming().stream().map(ProjectedOccurrenceResponse::from).toList(),
            p.projectedEndOfMonthBalance());
    }
}
