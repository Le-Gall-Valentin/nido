package com.nido.api.finance.infrastructure.persistence.repository;

import java.util.UUID;

/** JPQL constructor-expression projection: one member's still-encrypted share of a transaction. */
public record ContributorShareRow(UUID transactionId, UUID userId, String shareAmountEncrypted) {}
