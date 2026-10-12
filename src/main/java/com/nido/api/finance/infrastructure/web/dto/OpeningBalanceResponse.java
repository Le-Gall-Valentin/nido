package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.OpeningBalance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpeningBalanceResponse(BigDecimal amount, LocalDate asOf) {
    public static OpeningBalanceResponse from(OpeningBalance openingBalance) {
        return new OpeningBalanceResponse(openingBalance.amount(), openingBalance.asOf());
    }
}
