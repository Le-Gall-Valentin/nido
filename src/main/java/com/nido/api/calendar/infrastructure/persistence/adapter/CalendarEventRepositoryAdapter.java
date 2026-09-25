package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.infrastructure.config.CalendarEncryptorFactory;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventEntity;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventParticipantEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarEventJpaRepository;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarEventParticipantJpaRepository;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final CalendarEncryptorFactory encryptorFactory;

    public CalendarEventRepositoryAdapter(CalendarEventJpaRepository events,
                                          CalendarEventParticipantJpaRepository participants,
                                          CalendarEncryptorFactory encryptorFactory) {
        this.events = events;
        this.participants = participants;
        this.encryptorFactory = encryptorFactory;
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
    public Set<LocalDate> findDetachedSlots(UUID seriesId, LocalDate from, LocalDate to) {
        return new LinkedHashSet<>(events.findDetachedSlots(seriesId, from, to));
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
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        CalendarEventEntity entity = new CalendarEventEntity();
        entity.setSpaceId(command.spaceId());
        entity.setTitleEncrypted(encryptor.encrypt(command.title()));
        entity.setDescriptionEncrypted(encryptOrNull(encryptor, command.description()));
        entity.setLocationEncrypted(encryptOrNull(encryptor, command.location()));
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
        TextEncryptor encryptor = encryptorFactory.forSpace(entity.getSpaceId());
        entity.setTitleEncrypted(encryptor.encrypt(command.title()));
        entity.setDescriptionEncrypted(encryptOrNull(encryptor, command.description()));
        entity.setLocationEncrypted(encryptOrNull(encryptor, command.location()));
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
        if (participants.existsByEventIdAndUserId(eventId, userId)) {
            return;
        }
        CalendarEventParticipantEntity row = new CalendarEventParticipantEntity();
        row.setEventId(eventId);
        row.setUserId(userId);
        participants.save(row);
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

    private static String encryptOrNull(TextEncryptor encryptor, String value) {
        return value == null ? null : encryptor.encrypt(value);
    }

    private static String decryptOrNull(TextEncryptor encryptor, String value) {
        return value == null ? null : encryptor.decrypt(value);
    }

    private CalendarEvent toDomain(CalendarEventEntity e, List<UUID> participantIds) {
        TextEncryptor encryptor = encryptorFactory.forSpace(e.getSpaceId());
        return new CalendarEvent(
            e.getId(), e.getSpaceId(),
            encryptor.decrypt(e.getTitleEncrypted()),
            decryptOrNull(encryptor, e.getDescriptionEncrypted()),
            decryptOrNull(encryptor, e.getLocationEncrypted()),
            e.isAllDay(), e.getStartDate(), e.getStartTime(), e.getEndDate(), e.getEndTime(),
            e.getColor(), participantIds,
            e.getRecurringSeriesId(), e.getRecurringOriginalDate(),
            e.getCreatedBy(), e.getCreatedAt());
    }
}
