package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FinanceSavingsGoalJpaRepository extends JpaRepository<FinanceSavingsGoalEntity, UUID> {
    List<FinanceSavingsGoalEntity> findBySpaceId(UUID spaceId);

    // Held for the rest of the transaction: serializes concurrent contributions to the
    // same goal so two requests can't both read the same already-contributed total and
    // each add one that, together, push past the target — see
    // FinanceRecurringSeriesJpaRepository.lockMaterialization for the identical pattern.
    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockContribution(@Param("key") String key);
}
