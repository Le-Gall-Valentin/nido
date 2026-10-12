package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.domain.model.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPQL projection: an operation reduced to what a total is made of — its date, who handled it, its still-sealed amount
 * and its type, with the id its amount is sealed to and its shares hang from. No label: a total never needs one.
 */
public record LedgerRow(UUID transactionId, LocalDate date, UUID payerId, String amountEncrypted, TransactionType type) {}
