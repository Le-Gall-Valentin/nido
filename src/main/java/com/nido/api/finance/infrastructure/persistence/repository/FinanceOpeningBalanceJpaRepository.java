package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceOpeningBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FinanceOpeningBalanceJpaRepository extends JpaRepository<FinanceOpeningBalanceEntity, UUID> {
    Optional<FinanceOpeningBalanceEntity> findBySpaceId(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
}
