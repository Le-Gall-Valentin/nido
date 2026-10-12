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

/** How much one month moved one member's balance — a cache of its operations and settlements, kept by MonthTotalsWriter. */
@Entity
@Table(name = "finance_month_member_totals")
@Getter
@Setter
@NoArgsConstructor
public class FinanceMonthMemberTotalEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    public static final SealedColumn NET = SealedColumn.ofSpace("finance_month_member_totals", "net_encrypted");

    @Column(name = "net_encrypted", nullable = false)
    private String netEncrypted;
}
