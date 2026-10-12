package com.nido.api.finance.infrastructure.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code amount} may be negative: an overdraft is a balance too. */
public record OpeningBalanceRequest(@NotNull @Digits(integer = 12, fraction = 2) BigDecimal amount, @NotNull LocalDate asOf) {}
