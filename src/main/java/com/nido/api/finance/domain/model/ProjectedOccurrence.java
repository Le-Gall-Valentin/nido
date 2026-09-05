package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A future recurring occurrence computed in memory — never persisted. */
public record ProjectedOccurrence(UUID seriesId, String label, BigDecimal amount, TransactionType type, LocalDate date) {}
