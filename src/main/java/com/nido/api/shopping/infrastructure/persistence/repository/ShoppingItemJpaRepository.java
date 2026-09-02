package com.nido.api.shopping.infrastructure.persistence.repository;

import com.nido.api.shopping.infrastructure.persistence.entity.ShoppingItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShoppingItemJpaRepository extends JpaRepository<ShoppingItemEntity, UUID> {
    List<ShoppingItemEntity> findBySpaceIdOrderByPositionAsc(UUID spaceId);
    List<ShoppingItemEntity> findBySpaceIdAndDoneFalseOrderByPositionAsc(UUID spaceId);
    long countBySpaceIdAndCategoryId(UUID spaceId, UUID categoryId);
    void deleteBySpaceIdAndDoneTrue(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);

    @Modifying
    @Query("update ShoppingItemEntity i set i.categoryId = :toCategoryId where i.categoryId = :fromCategoryId")
    void reassignCategory(@Param("fromCategoryId") UUID fromCategoryId, @Param("toCategoryId") UUID toCategoryId);
}
