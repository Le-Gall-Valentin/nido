package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceSettlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinanceSettlementRecordJpaRepository extends JpaRepository<FinanceSettlementRecordEntity, UUID> {
    List<FinanceSettlementRecordEntity> findBySpaceId(UUID spaceId);
}
