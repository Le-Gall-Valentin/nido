package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SetOpeningBalanceCommand(UUID spaceId, BigDecimal amount, LocalDate asOf) {}
