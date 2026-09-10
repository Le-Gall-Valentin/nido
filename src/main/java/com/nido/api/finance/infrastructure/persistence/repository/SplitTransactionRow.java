package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.domain.model.TransactionType;

import java.util.UUID;

/**
 * JPQL constructor-expression projection for the balance fold: who handled the transaction,
 * its still-encrypted amount, the type that says which way it moves a balance, and the id
 * needed to attach its shares.
 *
 * <p>Stays in infrastructure rather than being a domain record — unlike
 * {@code RecurringSeriesSchedule}, it carries ciphertext, and the domain has no business
 * knowing that a stored amount is encrypted. The adapter decrypts it on the way out.
 */
public record SplitTransactionRow(UUID transactionId, UUID payerId, String amountEncrypted,
                                  TransactionType type) {}
