package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinanceTransactionJpaRepository extends JpaRepository<FinanceTransactionEntity, UUID> {
    /**
     * Newest first, the way a bank statement reads; on the same day, the latest entry goes on top.
     * Without the ORDER BY the order would be whichever the planner happened to read the rows in.
     */
    @Query("""
        SELECT t FROM FinanceTransactionEntity t
        WHERE t.spaceId = :spaceId AND t.date >= :from AND t.date <= :to
        ORDER BY t.date DESC, t.createdAt DESC
        """)
    List<FinanceTransactionEntity> findBySpaceIdAndDateBetween(
        @Param("spaceId") UUID spaceId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<FinanceTransactionEntity> findBySpaceId(UUID spaceId);


    /** The operations of a space dated within a range, reduced to what a total is made of; ciphertext untouched. */
    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.LedgerRow(
            t.id, t.date, t.payerId, t.amountEncrypted, t.type)
        from FinanceTransactionEntity t
        where t.spaceId = :spaceId and t.date >= :from and t.date <= :to
        """)
    List<LedgerRow> findLedgerRows(@Param("spaceId") UUID spaceId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** The same over the whole history of the space: only the building of its totals reads it. */
    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.LedgerRow(
            t.id, t.date, t.payerId, t.amountEncrypted, t.type)
        from FinanceTransactionEntity t
        where t.spaceId = :spaceId
        """)
    List<LedgerRow> findLedgerRowsBySpaceId(@Param("spaceId") UUID spaceId);
}
