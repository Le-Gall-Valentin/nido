package com.nido.api.finance.infrastructure.persistence.repository;

import java.util.UUID;

/**
 * JPQL constructor-expression projection: one member's still-sealed share of a transaction, with the id of its row,
 * which the share is sealed to.
 */
public record ContributorShareRow(UUID id, UUID transactionId, UUID userId, String shareAmountEncrypted) {}
