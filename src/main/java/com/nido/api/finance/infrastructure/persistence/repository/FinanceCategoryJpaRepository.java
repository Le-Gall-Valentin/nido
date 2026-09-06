package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FinanceCategoryJpaRepository extends JpaRepository<FinanceCategoryEntity, UUID> {
    List<FinanceCategoryEntity> findBySpaceId(UUID spaceId);
    boolean existsBySpaceId(UUID spaceId);

    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockForSeeding(@Param("key") String key);

    // Native, not JPQL: finance_transactions has no JPA entity yet at this point in the
    // build (it's introduced in a later task) but its table already exists (Task 3's
    // migrations), so a native query is the only option that doesn't create a forward
    // dependency between the two persistence packages.
    @Query(value = "SELECT COUNT(*) > 0 FROM finance_transactions WHERE category_id = :categoryId", nativeQuery = true)
    boolean existsTransactionForCategory(@Param("categoryId") UUID categoryId);
}
