package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryAmount(UUID categoryId, BigDecimal amount) {}
