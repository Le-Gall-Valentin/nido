package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FinanceTransactionContributorJpaRepository extends JpaRepository<FinanceTransactionContributorEntity, UUID> {
    List<FinanceTransactionContributorEntity> findByTransactionId(UUID transactionId);
    List<FinanceTransactionContributorEntity> findByTransactionIdInOrderByTransactionIdAsc(Collection<UUID> transactionIds);
    void deleteByTransactionId(UUID transactionId);


    /** The shares of the operations of a space dated within a range, projected — no entity, ciphertext untouched. */
    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.ContributorShareRow(
            c.id, c.transactionId, c.userId, c.shareAmountEncrypted)
        from FinanceTransactionContributorEntity c
        join FinanceTransactionEntity t on t.id = c.transactionId
        where t.spaceId = :spaceId and t.date >= :from and t.date <= :to
        """)
    List<ContributorShareRow> findShareRows(@Param("spaceId") UUID spaceId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** The same over the whole history of the space: only the building of its totals reads it. */
    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.ContributorShareRow(
            c.id, c.transactionId, c.userId, c.shareAmountEncrypted)
        from FinanceTransactionContributorEntity c
        join FinanceTransactionEntity t on t.id = c.transactionId
        where t.spaceId = :spaceId
        """)
    List<ContributorShareRow> findShareRowsBySpaceId(@Param("spaceId") UUID spaceId);
}
