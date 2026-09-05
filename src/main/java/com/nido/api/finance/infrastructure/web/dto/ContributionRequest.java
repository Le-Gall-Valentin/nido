package com.nido.api.finance.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code shareAmount} null requests an equal split; see {@code ContributionSplitter}. */
public record ContributionRequest(@NotNull UUID memberId, BigDecimal shareAmount) {}
