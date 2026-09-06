package com.nido.api.finance.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "finance_recurring_series_contributors")
@Getter
@Setter
@NoArgsConstructor
public class FinanceRecurringSeriesContributorEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "share_amount_encrypted", nullable = false)
    private String shareAmountEncrypted;
}
