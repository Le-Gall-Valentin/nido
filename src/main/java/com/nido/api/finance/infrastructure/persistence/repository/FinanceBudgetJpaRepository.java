package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceBudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceBudgetJpaRepository extends JpaRepository<FinanceBudgetEntity, UUID> {
    List<FinanceBudgetEntity> findBySpaceId(UUID spaceId);
    Optional<FinanceBudgetEntity> findByCategoryId(UUID categoryId);
    void deleteByCategoryId(UUID categoryId);
}
