package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberBalance(UUID memberId, BigDecimal net) {}
