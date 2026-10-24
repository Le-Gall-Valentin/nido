package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.UUID;

public record Transaction(
    UUID id, UUID spaceId, String label, BigDecimal amount, TransactionType type, UUID categoryId,
    LocalDate date, UUID payerId, List<Contribution> contributors, UUID recurringSeriesId, Instant createdAt
) {
    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(contributors, "contributors");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** The members it concerns — whoever paid (or received, for an income), then whoever shares it; none when unshared. */
    public SequencedSet<UUID> concernedMembers() {
        return Contribution.concerned(payerId, contributors);
    }

    /** Whether it reads differently to a member than {@code other}: an amount of another scale is the same amount. */
    public boolean differsFrom(Transaction other) {
        return !label.equals(other.label)
            || amount.compareTo(other.amount) != 0
            || type != other.type
            || !categoryId.equals(other.categoryId)
            || !date.equals(other.date)
            || !Objects.equals(payerId, other.payerId)
            || !Contribution.sameShares(contributors, other.contributors);
    }
}
