package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceOpeningBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FinanceOpeningBalanceJpaRepository extends JpaRepository<FinanceOpeningBalanceEntity, UUID> {
    Optional<FinanceOpeningBalanceEntity> findBySpaceId(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);

    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockForWrite(@Param("key") String key);
}
