package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinanceTransactionJpaRepository extends JpaRepository<FinanceTransactionEntity, UUID> {
    @Query("SELECT t FROM FinanceTransactionEntity t WHERE t.spaceId = :spaceId AND t.date >= :from AND t.date <= :to")
    List<FinanceTransactionEntity> findBySpaceIdAndDateBetween(
        @Param("spaceId") UUID spaceId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<FinanceTransactionEntity> findBySpaceId(UUID spaceId);

    /**
     * The transactions a balance is made of, projected and filtered.
     *
     * <p>The two conditions mirror the guard {@code BalanceCalculator} already applies: a
     * transaction with no payer or no contributors moves no balance. Applying them here means
     * a household's solo expenses — most of them, typically — are never loaded or decrypted
     * just to be skipped.
     */
    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.SplitTransactionRow(
            t.id, t.payerId, t.amountEncrypted, t.type)
        from FinanceTransactionEntity t
        where t.spaceId = :spaceId
          and t.payerId is not null
          and exists (
            select 1 from FinanceTransactionContributorEntity c where c.transactionId = t.id)
        """)
    List<SplitTransactionRow> findSplitRowsBySpaceId(@Param("spaceId") UUID spaceId);
}
