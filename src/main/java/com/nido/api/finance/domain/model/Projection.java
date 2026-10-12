package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code recordedEndOfMonthBalance} is everything recorded up to the month end, carried over from the months before;
 * {@code projectedEndOfMonthBalance} adds the recurring operations still to come in the month.
 */
public record Projection(BigDecimal recordedEndOfMonthBalance, List<ProjectedOccurrence> upcoming, BigDecimal projectedEndOfMonthBalance) {}
