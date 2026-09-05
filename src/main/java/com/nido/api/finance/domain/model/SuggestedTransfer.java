package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record SuggestedTransfer(UUID fromMemberId, UUID toMemberId, BigDecimal amount) {}
