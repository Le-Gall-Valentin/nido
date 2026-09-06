package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceSettlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FinanceSettlementRecordJpaRepository extends JpaRepository<FinanceSettlementRecordEntity, UUID> {
    List<FinanceSettlementRecordEntity> findBySpaceId(UUID spaceId);

    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockForSettlement(@Param("key") String key);
}
