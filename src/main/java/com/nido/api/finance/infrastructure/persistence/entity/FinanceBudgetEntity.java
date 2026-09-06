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
@Table(name = "finance_budgets")
@Getter
@Setter
@NoArgsConstructor
public class FinanceBudgetEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "monthly_limit_encrypted", nullable = false)
    private String monthlyLimitEncrypted;
}
