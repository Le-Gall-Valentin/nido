package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.UUID;

public record RecurringTransactionSeries(
    UUID id, UUID spaceId, String label, BigDecimal amount, TransactionType type, UUID categoryId,
    UUID payerId, List<Contribution> contributors, RecurrenceInterval intervalType, int intervalCount,
    LocalDate anchorDate, LocalDate endDate, LocalDate lastMaterializedDate
) {
    public RecurringTransactionSeries {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(contributors, "contributors");
        Objects.requireNonNull(intervalType, "intervalType");
        Objects.requireNonNull(anchorDate, "anchorDate");
    }

    /** The members it concerns — whoever pays (or receives), then whoever shares it; none when unshared. */
    public SequencedSet<UUID> concernedMembers() {
        return Contribution.concerned(payerId, contributors);
    }

    /**
     * Whether it reads differently to a member than {@code other}. How far it has been materialized is bookkeeping,
     * not something a member changed.
     */
    public boolean differsFrom(RecurringTransactionSeries other) {
        return !label.equals(other.label)
            || amount.compareTo(other.amount) != 0
            || type != other.type
            || !categoryId.equals(other.categoryId)
            || !Objects.equals(payerId, other.payerId)
            || !Contribution.sameShares(contributors, other.contributors)
            || intervalType != other.intervalType
            || intervalCount != other.intervalCount
            || !anchorDate.equals(other.anchorDate)
            || !Objects.equals(endDate, other.endDate);
    }
}
