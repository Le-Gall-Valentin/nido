package com.nido.api.shopping.infrastructure.persistence.repository;

import com.nido.api.shopping.infrastructure.persistence.entity.ShoppingCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShoppingCategoryJpaRepository extends JpaRepository<ShoppingCategoryEntity, UUID> {
    List<ShoppingCategoryEntity> findBySpaceIdOrderByPositionAsc(UUID spaceId);
    boolean existsBySpaceId(UUID spaceId);
    long countBySpaceId(UUID spaceId);

    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockForSeeding(@Param("key") String key);
}
