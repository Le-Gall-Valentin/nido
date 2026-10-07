package com.nido.api.finance.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
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

    public static final SealedColumn SHARE_AMOUNT = SealedColumn.throughParent("finance_recurring_series_contributors", "share_amount_encrypted",
        "series_id", "finance_recurring_transaction_series");

    @Column(name = "share_amount_encrypted", nullable = false)
    private String shareAmountEncrypted;
}
