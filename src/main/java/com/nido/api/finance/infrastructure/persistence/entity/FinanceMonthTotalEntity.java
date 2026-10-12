package com.nido.api.finance.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** The net of one month of a space — a cache of its operations, kept by MonthTotalsWriter. */
@Entity
@Table(name = "finance_month_totals")
@Getter
@Setter
@NoArgsConstructor
public class FinanceMonthTotalEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    public static final SealedColumn NET = SealedColumn.ofSpace("finance_month_totals", "net_encrypted");

    @Column(name = "net_encrypted", nullable = false)
    private String netEncrypted;
}
