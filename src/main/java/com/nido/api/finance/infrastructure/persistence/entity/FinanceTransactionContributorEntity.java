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
@Table(name = "finance_transaction_contributors")
@Getter
@Setter
@NoArgsConstructor
public class FinanceTransactionContributorEntity extends AssignedUuidEntity {

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public static final SealedColumn SHARE_AMOUNT = SealedColumn.throughParent("finance_transaction_contributors", "share_amount_encrypted",
        "transaction_id", "finance_transactions");

    @Column(name = "share_amount_encrypted", nullable = false)
    private String shareAmountEncrypted;
}
