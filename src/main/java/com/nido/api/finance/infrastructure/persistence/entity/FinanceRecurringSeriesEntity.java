package com.nido.api.finance.infrastructure.persistence.entity;

import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_recurring_transaction_series")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FinanceRecurringSeriesEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "label_encrypted", nullable = false)
    private String labelEncrypted;

    @Column(name = "amount_encrypted", nullable = false)
    private String amountEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "payer_id")
    private UUID payerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", nullable = false, length = 10)
    private RecurrenceInterval intervalType;

    @Column(name = "interval_count", nullable = false)
    private int intervalCount;

    @Column(name = "anchor_date", nullable = false)
    private LocalDate anchorDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "last_materialized_date")
    private LocalDate lastMaterializedDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
