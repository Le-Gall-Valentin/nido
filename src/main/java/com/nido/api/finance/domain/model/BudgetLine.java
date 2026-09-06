package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetLine(UUID categoryId, BigDecimal monthlyLimit, BigDecimal spent) {}
