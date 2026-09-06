package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinanceTransactionJpaRepository extends JpaRepository<FinanceTransactionEntity, UUID> {
    @Query("SELECT t FROM FinanceTransactionEntity t WHERE t.spaceId = :spaceId AND t.date >= :from AND t.date <= :to")
    List<FinanceTransactionEntity> findBySpaceIdAndDateBetween(
        @Param("spaceId") UUID spaceId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<FinanceTransactionEntity> findBySpaceId(UUID spaceId);
}
