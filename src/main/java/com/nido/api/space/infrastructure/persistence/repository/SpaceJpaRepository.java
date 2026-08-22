package com.nido.api.space.infrastructure.persistence.repository;

import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpaceJpaRepository extends JpaRepository<SpaceEntity, UUID> {
    Optional<SpaceEntity> findByPersonalOwnerId(UUID personalOwnerId);

    org.springframework.data.domain.Page<SpaceEntity> findAllByOrderByCreatedAtDesc(
        org.springframework.data.domain.Pageable pageable);
}
