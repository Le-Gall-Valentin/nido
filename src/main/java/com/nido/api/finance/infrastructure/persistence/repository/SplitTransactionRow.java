package com.nido.api.finance.infrastructure.persistence.repository;

import java.util.UUID;

/**
 * JPQL constructor-expression projection for the balance fold: a transaction's payer and its
 * still-encrypted amount, with the id needed to attach its shares.
 *
 * <p>Stays in infrastructure rather than being a domain record — unlike
 * {@code RecurringSeriesSchedule}, it carries ciphertext, and the domain has no business
 * knowing that a stored amount is encrypted. The adapter decrypts it on the way out.
 */
public record SplitTransactionRow(UUID transactionId, UUID payerId, String amountEncrypted) {}
