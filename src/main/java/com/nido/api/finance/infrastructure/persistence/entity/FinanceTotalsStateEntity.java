package com.nido.api.finance.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Whether a space's month totals are built, and at which version — see MonthTotalsWriter. Holds no sealed value. */
@Entity
@Table(name = "finance_totals_states")
@Getter
@Setter
@NoArgsConstructor
public class FinanceTotalsStateEntity {

    @Id
    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false)
    private int version;
}
