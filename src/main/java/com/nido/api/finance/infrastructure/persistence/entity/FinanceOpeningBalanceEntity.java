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
@Table(name = "finance_opening_balances")
@Getter
@Setter
@NoArgsConstructor
public class FinanceOpeningBalanceEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    public static final SealedColumn AMOUNT = SealedColumn.ofSpace("finance_opening_balances", "amount_encrypted");

    @Column(name = "amount_encrypted", nullable = false)
    private String amountEncrypted;

    @Column(name = "as_of", nullable = false)
    private LocalDate asOf;
}
