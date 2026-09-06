package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceRecurringSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinanceRecurringSeriesJpaRepository extends JpaRepository<FinanceRecurringSeriesEntity, UUID> {
    List<FinanceRecurringSeriesEntity> findBySpaceId(UUID spaceId);

    // Written as JPQL rather than a derived query name: a name like
    // "findBySpaceIdAndEndDateIsNullOrEndDateGreaterThanEqual" would parenthesize as
    // "(spaceId = ?1 AND endDate IS NULL) OR (endDate >= ?2)", silently matching every
    // other space's non-expired series too — the explicit query keeps spaceId anded
    // across the whole OR.
    @Query("SELECT s FROM FinanceRecurringSeriesEntity s WHERE s.spaceId = :spaceId AND (s.endDate IS NULL OR s.endDate >= :asOf)")
    List<FinanceRecurringSeriesEntity> findActiveBySpaceId(@Param("spaceId") UUID spaceId, @Param("asOf") LocalDate asOf);

    // Held for the rest of the transaction: serializes concurrent materialization for the
    // same space so two requests can't both read the same lastMaterializedDate and each
    // insert the same occurrence — see finance_transactions' recurring-series/date unique
    // constraint for the belt-and-suspenders backstop.
    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockMaterialization(@Param("key") String key);
}
