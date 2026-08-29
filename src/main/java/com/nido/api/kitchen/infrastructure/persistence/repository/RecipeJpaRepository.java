package com.nido.api.kitchen.infrastructure.persistence.repository;

import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecipeJpaRepository extends JpaRepository<RecipeEntity, UUID> {
    List<RecipeEntity> findBySpaceIdOrderByNameAsc(UUID spaceId);

    @Modifying(clearAutomatically = true)
    @Query("update RecipeEntity r set r.favorite = :favorite where r.id = :id")
    int setFavorite(@Param("id") UUID id, @Param("favorite") boolean favorite);
}
