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
@Table(name = "finance_budgets")
@Getter
@Setter
@NoArgsConstructor
public class FinanceBudgetEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    public static final SealedColumn MONTHLY_LIMIT = SealedColumn.ofSpace("finance_budgets", "monthly_limit_encrypted");

    @Column(name = "monthly_limit_encrypted", nullable = false)
    private String monthlyLimitEncrypted;
}
