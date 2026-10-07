package com.nido.api.finance.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "finance_recurring_series_contributors")
@Getter
@Setter
@NoArgsConstructor
public class FinanceRecurringSeriesContributorEntity extends AssignedUuidEntity {

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "share_amount_encrypted", nullable = false)
    private String shareAmountEncrypted;
}
