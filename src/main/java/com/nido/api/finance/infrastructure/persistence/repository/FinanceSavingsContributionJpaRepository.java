package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsContributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinanceSavingsContributionJpaRepository extends JpaRepository<FinanceSavingsContributionEntity, UUID> {
    List<FinanceSavingsContributionEntity> findByGoalId(UUID goalId);
}
