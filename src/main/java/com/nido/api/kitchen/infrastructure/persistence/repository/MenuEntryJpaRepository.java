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

    // Serializes position assignment for a given (space, date) within the caller's
    // transaction so concurrent inserts can't read the same count and collide on
    // the same position: the lock is held until the transaction commits/rolls back.
    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockPositionAssignment(@Param("key") String key);

    @Query("""
        select new com.nido.api.kitchen.infrastructure.persistence.repository.LastPlannedOn(m.recipeId, max(m.date))
        from MenuEntryEntity m
        where m.spaceId = :spaceId
        group by m.recipeId
        """)
    List<LastPlannedOn> findLastPlannedOnBySpaceId(@Param("spaceId") UUID spaceId);
}
