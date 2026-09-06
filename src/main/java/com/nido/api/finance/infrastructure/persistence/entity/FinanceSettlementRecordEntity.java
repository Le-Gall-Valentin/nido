package com.nido.api.finance.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_settlement_records")
@Getter
@Setter
@NoArgsConstructor
public class FinanceSettlementRecordEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    @Column(name = "amount_encrypted", nullable = false)
    private String amountEncrypted;

    @Column(name = "settled_date", nullable = false)
    private LocalDate settledDate;
}
