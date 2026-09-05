package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record Projection(BigDecimal actualBalanceSoFar, List<ProjectedOccurrence> upcoming, BigDecimal projectedEndOfMonthBalance) {}
