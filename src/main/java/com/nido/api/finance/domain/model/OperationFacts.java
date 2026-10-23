package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What a mail tells about a one-off operation, its names already read: the category's label, the payer's and every
 * contributor's name — null for one that can no longer be named.
 */
public record OperationFacts(String label, BigDecimal amount, TransactionType type, String categoryLabel, LocalDate date,
                             UUID payerId, String payerName, List<MemberShare> shares) {

    /** The share {@code memberId} holds; null when they do not share it. */
    public BigDecimal shareOf(UUID memberId) {
        return Shares.of(shares, memberId);
    }

    public boolean paidBy(UUID memberId) {
        return memberId.equals(payerId);
    }
}
