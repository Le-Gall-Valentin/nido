package com.nido.api.kitchen.infrastructure.persistence.repository;

import com.nido.api.kitchen.infrastructure.persistence.entity.MenuEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MenuEntryJpaRepository extends JpaRepository<MenuEntryEntity, UUID> {

    List<MenuEntryEntity> findBySpaceIdAndDateBetweenOrderByDateAscPositionAsc(
        UUID spaceId, LocalDate from, LocalDate to);

    long countBySpaceIdAndDate(UUID spaceId, LocalDate date);

    @Query("""
        select new com.nido.api.kitchen.infrastructure.persistence.repository.LastPlannedOn(m.recipeId, max(m.date))
        from MenuEntryEntity m
        where m.spaceId = :spaceId
        group by m.recipeId
        """)
    List<LastPlannedOn> findLastPlannedOnBySpaceId(@Param("spaceId") UUID spaceId);
}
