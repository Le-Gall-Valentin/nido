package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record UpdateTransactionCommand(
    UUID transactionId, UUID spaceId, String label, BigDecimal amount, TransactionType type, UUID categoryId,
    LocalDate date, UUID payerId, List<ContributionInput> contributors
) {
    public UpdateTransactionCommand {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(contributors, "contributors");
    }
}
