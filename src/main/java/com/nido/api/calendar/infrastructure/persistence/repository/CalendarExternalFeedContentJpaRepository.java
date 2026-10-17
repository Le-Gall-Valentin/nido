package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CalendarExternalFeedContentJpaRepository extends JpaRepository<CalendarExternalFeedContentEntity, UUID> {
    Optional<CalendarExternalFeedContentEntity> findByFeedId(UUID feedId);
}
