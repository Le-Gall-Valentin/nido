package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceTotalsStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FinanceTotalsStateJpaRepository extends JpaRepository<FinanceTotalsStateEntity, UUID> {

    /** The space's totals lock, held until the transaction ends — see MonthTotalsWriter. */
    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockTotals(@Param("key") String key);

    @Query("select s.version from FinanceTotalsStateEntity s where s.spaceId = :spaceId")
    Optional<Integer> findVersion(@Param("spaceId") UUID spaceId);

    @Modifying
    @Query(value = """
        insert into finance_totals_states (space_id, version) values (:spaceId, :version)
        on conflict (space_id) do update set version = excluded.version
        """, nativeQuery = true)
    void saveVersion(@Param("spaceId") UUID spaceId, @Param("version") int version);
}
