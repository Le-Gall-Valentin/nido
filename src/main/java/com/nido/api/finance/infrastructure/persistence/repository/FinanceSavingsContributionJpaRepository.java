package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsContributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FinanceSavingsContributionJpaRepository extends JpaRepository<FinanceSavingsContributionEntity, UUID> {
    List<FinanceSavingsContributionEntity> findByGoalIdOrderByContributedDateDesc(UUID goalId);

    /** Only the contributions to goals of that space: another space's goal has none here. */
    @Query("""
        SELECT c FROM FinanceSavingsContributionEntity c, FinanceSavingsGoalEntity g
        WHERE g.id = c.goalId AND g.spaceId = :spaceId AND c.goalId IN :goalIds
        ORDER BY c.contributedDate DESC""")
    List<FinanceSavingsContributionEntity> findOfSpaceByGoalIds(@Param("spaceId") UUID spaceId, @Param("goalIds") List<UUID> goalIds);
}
