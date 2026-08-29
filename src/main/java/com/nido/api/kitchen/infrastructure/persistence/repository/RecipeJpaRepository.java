package com.nido.api.kitchen.infrastructure.persistence.repository;

import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeJpaRepository extends JpaRepository<RecipeEntity, UUID> {
    List<RecipeEntity> findBySpaceIdOrderByNameAsc(UUID spaceId);
}
