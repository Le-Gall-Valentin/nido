package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** One suggested transfer seen from the caller's side. */
public record BalanceWithMember(UUID memberId, BigDecimal amount, BalanceDirection direction) {
}
