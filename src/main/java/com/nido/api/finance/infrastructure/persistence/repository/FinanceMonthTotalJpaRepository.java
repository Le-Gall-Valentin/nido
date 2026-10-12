package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceMonthTotalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceMonthTotalJpaRepository extends JpaRepository<FinanceMonthTotalEntity, UUID> {

    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.MonthNetRow(t.id, t.monthStart, t.netEncrypted)
        from FinanceMonthTotalEntity t
        where t.spaceId = :spaceId
        """)
    List<MonthNetRow> findNetRows(@Param("spaceId") UUID spaceId);

    @Modifying(flushAutomatically = true)
    @Query("delete from FinanceMonthTotalEntity t where t.spaceId = :spaceId and t.monthStart in :monthStarts")
    void deleteMonths(@Param("spaceId") UUID spaceId, @Param("monthStarts") Collection<LocalDate> monthStarts);

    @Modifying(flushAutomatically = true)
    @Query("delete from FinanceMonthTotalEntity t where t.spaceId = :spaceId")
    void deleteAllOfSpace(@Param("spaceId") UUID spaceId);

    /** The space's totals lock, held until the transaction ends — see MonthTotalsWriter. */
    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockTotals(@Param("key") String key);

    @Query(value = "select version from finance_totals_states where space_id = :spaceId", nativeQuery = true)
    Optional<Integer> findTotalsVersion(@Param("spaceId") UUID spaceId);

    @Modifying
    @Query(value = """
        insert into finance_totals_states (space_id, version) values (:spaceId, :version)
        on conflict (space_id) do update set version = excluded.version
        """, nativeQuery = true)
    void saveTotalsVersion(@Param("spaceId") UUID spaceId, @Param("version") int version);
}
