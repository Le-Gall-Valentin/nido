package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceRecurringSeriesContributorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinanceRecurringSeriesContributorJpaRepository extends JpaRepository<FinanceRecurringSeriesContributorEntity, UUID> {
    List<FinanceRecurringSeriesContributorEntity> findBySeriesId(UUID seriesId);
    void deleteBySeriesId(UUID seriesId);
}
