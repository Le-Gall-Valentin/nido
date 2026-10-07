package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventEntity;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventParticipantEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarEventJpaRepository;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarEventParticipantJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CalendarEventRepositoryAdapter implements CalendarEventRepository {

    private final CalendarEventJpaRepository events;
    private final CalendarEventParticipantJpaRepository participants;
    private final SpaceSealers sealers;

    public CalendarEventRepositoryAdapter(CalendarEventJpaRepository events,
                                          CalendarEventParticipantJpaRepository participants,
                                          SpaceSealers sealers) {
        this.events = events;
        this.participants = participants;
        this.sealers = sealers;
    }

    @Override
    public Optional<CalendarEvent> findById(UUID eventId) {
        return events.findById(eventId).map(e -> toDomain(e, participantIdsOf(e.getId())));
    }

    @Override
    public List<CalendarEvent> findBySpaceIdOverlapping(UUID spaceId, LocalDate from, LocalDate to) {
        List<CalendarEventEntity> found = events.findOverlapping(spaceId, from, to);
        if (found.isEmpty()) {
            return List.of();
        }
        // One query for every event's participants rather than one per event: a month window can
        // return hundreds of rows, and the per-row version is the N+1 this method exists to avoid.
        Map<UUID, List<UUID>> byEvent = participants
            .findByEventIdIn(found.stream().map(CalendarEventEntity::getId).toList()).stream()
            .collect(Collectors.groupingBy(CalendarEventParticipantEntity::getEventId,
                Collectors.mapping(CalendarEventParticipantEntity::getUserId, Collectors.toList())));
        return found.stream()
            .map(e -> toDomain(e, byEvent.getOrDefault(e.getId(), List.of())))
            .toList();
    }

    @Override
    public Map<UUID, Set<LocalDate>> findDetachedSlotsOfEach(Collection<UUID> seriesIds, LocalDate from, LocalDate to) {
        return events.findDetachedSlotsOfEach(seriesIds, from, to).stream()
            .collect(Collectors.groupingBy(CalendarEventJpaRepository.SeriesSlot::getSeriesId,
                Collectors.mapping(CalendarEventJpaRepository.SeriesSlot::getOriginalDate, Collectors.toSet())));
    }

    @Override
    public Optional<CalendarEvent> findBySeriesAndOriginalDate(UUID seriesId, LocalDate originalDate) {
        return events.findByRecurringSeriesIdAndRecurringOriginalDate(seriesId, originalDate)
            .map(e -> toDomain(e, participantIdsOf(e.getId())));
    }

    @Override
    public List<CalendarEvent> findDetachedOf(UUID seriesId) {
        List<CalendarEventEntity> found = events.findByRecurringSeriesId(seriesId);
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<UUID>> byEvent = participants
            .findByEventIdIn(found.stream().map(CalendarEventEntity::getId).toList()).stream()
            .collect(Collectors.groupingBy(CalendarEventParticipantEntity::getEventId,
                Collectors.mapping(CalendarEventParticipantEntity::getUserId, Collectors.toList())));
        return found.stream().map(e -> toDomain(e, byEvent.getOrDefault(e.getId(), List.of()))).toList();
    }

    @Override
    @Transactional
    public void relink(UUID eventId, UUID seriesId, LocalDate originalDate) {
        events.relink(eventId, seriesId, originalDate);
    }

    @Override
    @Transactional
    public CalendarEvent create(CreateEventCommand command) {
        SpaceSealer sealer = sealers.forSpace(command.spaceId());
        CalendarEventEntity entity = new CalendarEventEntity();
        entity.setSpaceId(command.spaceId());
        entity.setTitleEncrypted(sealer.seal(CalendarEventEntity.TITLE, entity.getId(), command.title()));
        entity.setDescriptionEncrypted(sealer.sealNullable(CalendarEventEntity.DESCRIPTION, entity.getId(), command.description()));
        entity.setLocationEncrypted(sealer.sealNullable(CalendarEventEntity.LOCATION, entity.getId(), command.location()));
        entity.setAllDay(command.allDay());
        entity.setStartDate(command.startDate());
        entity.setStartTime(command.startTime());
        entity.setEndDate(command.endDate());
        entity.setEndTime(command.endTime());
        entity.setColor(command.color());
        entity.setRecurringSeriesId(command.recurringSeriesId());
        entity.setRecurringOriginalDate(command.recurringOriginalDate());
        entity.setCreatedBy(command.createdBy());
        CalendarEventEntity saved = events.saveAndFlush(entity);
        replaceParticipants(saved.getId(), command.participantIds());
        return toDomain(saved, List.copyOf(command.participantIds()));
    }

    @Override
    @Transactional
    public CalendarEvent update(UpdateEventCommand command) {
        CalendarEventEntity entity = events.findById(command.eventId())
            .orElseThrow(CalendarException.EventNotFound::new);
        SpaceSealer sealer = sealers.forSpace(entity.getSpaceId());
        entity.setTitleEncrypted(sealer.seal(CalendarEventEntity.TITLE, entity.getId(), command.title()));
        entity.setDescriptionEncrypted(sealer.sealNullable(CalendarEventEntity.DESCRIPTION, entity.getId(), command.description()));
        entity.setLocationEncrypted(sealer.sealNullable(CalendarEventEntity.LOCATION, entity.getId(), command.location()));
        entity.setAllDay(command.allDay());
        entity.setStartDate(command.startDate());
        entity.setStartTime(command.startTime());
        entity.setEndDate(command.endDate());
        entity.setEndTime(command.endTime());
        entity.setColor(command.color());
        CalendarEventEntity saved = events.saveAndFlush(entity);
        replaceParticipants(saved.getId(), command.participantIds());
        return toDomain(saved, List.copyOf(command.participantIds()));
    }

    @Override
    @Transactional
    public void delete(UUID eventId) {
        events.deleteById(eventId);
    }

    @Override
    @Transactional
    public void addParticipant(UUID eventId, UUID userId) {
        participants.insertIfAbsent(eventId, userId);
    }

    @Override
    @Transactional
    public void removeParticipant(UUID eventId, UUID userId) {
        participants.deleteByEventIdAndUserId(eventId, userId);
    }

    private void replaceParticipants(UUID eventId, List<UUID> userIds) {
        participants.deleteByEventId(eventId);
        participants.flush();
        for (UUID userId : new LinkedHashSet<>(userIds)) {
            CalendarEventParticipantEntity row = new CalendarEventParticipantEntity();
            row.setEventId(eventId);
            row.setUserId(userId);
            participants.save(row);
        }
    }

    private List<UUID> participantIdsOf(UUID eventId) {
        return participants.findByEventId(eventId).stream()
            .map(CalendarEventParticipantEntity::getUserId).toList();
    }

    private CalendarEvent toDomain(CalendarEventEntity e, List<UUID> participantIds) {
        SpaceSealer sealer = sealers.forSpace(e.getSpaceId());
        return new CalendarEvent(
            e.getId(), e.getSpaceId(),
            sealer.open(CalendarEventEntity.TITLE, e.getId(), e.getTitleEncrypted()),
            sealer.openNullable(CalendarEventEntity.DESCRIPTION, e.getId(), e.getDescriptionEncrypted()),
            sealer.openNullable(CalendarEventEntity.LOCATION, e.getId(), e.getLocationEncrypted()),
            e.isAllDay(), e.getStartDate(), e.getStartTime(), e.getEndDate(), e.getEndTime(),
            e.getColor(), participantIds,
            e.getRecurringSeriesId(), e.getRecurringOriginalDate(),
            e.getCreatedBy(), e.getCreatedAt());
    }
}
