package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinanceSavingsGoalJpaRepository extends JpaRepository<FinanceSavingsGoalEntity, UUID> {
    List<FinanceSavingsGoalEntity> findBySpaceId(UUID spaceId);
}
