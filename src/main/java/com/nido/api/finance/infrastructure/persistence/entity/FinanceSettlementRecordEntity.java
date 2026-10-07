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

@Entity
@Table(name = "finance_settlement_records")
@Getter
@Setter
@NoArgsConstructor
public class FinanceSettlementRecordEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    public static final SealedColumn AMOUNT = SealedColumn.ofSpace("finance_settlement_records", "amount_encrypted");

    @Column(name = "amount_encrypted", nullable = false)
    private String amountEncrypted;

    @Column(name = "settled_date", nullable = false)
    private LocalDate settledDate;
}
