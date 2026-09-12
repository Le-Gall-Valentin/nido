package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FinanceTransactionContributorJpaRepository extends JpaRepository<FinanceTransactionContributorEntity, UUID> {
    List<FinanceTransactionContributorEntity> findByTransactionId(UUID transactionId);
    List<FinanceTransactionContributorEntity> findByTransactionIdInOrderByTransactionIdAsc(Collection<UUID> transactionIds);
    void deleteByTransactionId(UUID transactionId);

    /** The shares of the given transactions, projected — no entity, no label, ciphertext untouched. */
    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.ContributorShareRow(
            c.transactionId, c.userId, c.shareAmountEncrypted)
        from FinanceTransactionContributorEntity c
        where c.transactionId in :transactionIds
        """)
    List<ContributorShareRow> findShareRowsByTransactionIdIn(@Param("transactionIds") Collection<UUID> transactionIds);
}
