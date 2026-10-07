package com.nido.api.finance.infrastructure.persistence.entity;

import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_transactions")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FinanceTransactionEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    public static final SealedColumn LABEL = SealedColumn.ofSpace("finance_transactions", "label_encrypted");

    @Column(name = "label_encrypted", nullable = false)
    private String labelEncrypted;

    public static final SealedColumn AMOUNT = SealedColumn.ofSpace("finance_transactions", "amount_encrypted");

    @Column(name = "amount_encrypted", nullable = false)
    private String amountEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "payer_id")
    private UUID payerId;

    @Column(name = "recurring_series_id")
    private UUID recurringSeriesId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
