package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What a mail tells about a recurring transaction, its names already read — the shares are those of each occurrence.
 *
 * @param until the last day an occurrence may fall on; null when it never ends
 */
public record SeriesFacts(String label, BigDecimal amount, TransactionType type, String categoryLabel, UUID payerId,
                          String payerName, List<MemberShare> shares, RecurrenceInterval interval, int intervalCount,
                          LocalDate from, LocalDate until) {

    /** The share {@code memberId} holds of each occurrence; null when they do not share it. */
    public BigDecimal shareOf(UUID memberId) {
        return Shares.of(shares, memberId);
    }

    public boolean paidBy(UUID memberId) {
        return memberId.equals(payerId);
    }
}
