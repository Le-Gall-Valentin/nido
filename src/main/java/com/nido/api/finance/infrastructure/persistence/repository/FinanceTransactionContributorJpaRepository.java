package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FinanceTransactionContributorJpaRepository extends JpaRepository<FinanceTransactionContributorEntity, UUID> {
    List<FinanceTransactionContributorEntity> findByTransactionId(UUID transactionId);
    List<FinanceTransactionContributorEntity> findByTransactionIdInOrderByTransactionIdAsc(Collection<UUID> transactionIds);
    void deleteByTransactionId(UUID transactionId);
}
