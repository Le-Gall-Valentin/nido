package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.calendar.infrastructure.config.CalendarEncryptorFactory;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesEntity;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesParticipantEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarRecurringEventSeriesJpaRepository;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarRecurringEventSeriesParticipantJpaRepository;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RecurringEventSeriesRepositoryAdapter implements RecurringEventSeriesRepository {

    private final CalendarRecurringEventSeriesJpaRepository series;
    private final CalendarRecurringEventSeriesParticipantJpaRepository participants;
    private final CalendarEncryptorFactory encryptorFactory;

    public RecurringEventSeriesRepositoryAdapter(
            CalendarRecurringEventSeriesJpaRepository series,
            CalendarRecurringEventSeriesParticipantJpaRepository participants,
            CalendarEncryptorFactory encryptorFactory) {
        this.series = series;
        this.participants = participants;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public Optional<RecurringEventSeries> findById(UUID seriesId) {
        return series.findById(seriesId).map(e -> toDomain(e, participantIdsOf(e.getId())));
    }

    @Override
    public List<RecurringEventSeries> findBySpaceId(UUID spaceId) {
        List<CalendarRecurringEventSeriesEntity> found = series.findBySpaceIdOrderByAnchorDate(spaceId);
        if (found.isEmpty()) {
            return List.of();
        }
        // Batched for the same N+1 reason as the events adapter: every occurrence read walks
        // every series of the space, so a per-series participant query would scale with the space.
        Map<UUID, List<UUID>> bySeries = participants
            .findBySeriesIdIn(found.stream().map(CalendarRecurringEventSeriesEntity::getId).toList()).stream()
            .collect(Collectors.groupingBy(CalendarRecurringEventSeriesParticipantEntity::getSeriesId,
                Collectors.mapping(CalendarRecurringEventSeriesParticipantEntity::getUserId, Collectors.toList())));
        return found.stream().map(e -> toDomain(e, bySeries.getOrDefault(e.getId(), List.of()))).toList();
    }

    @Override
    @Transactional
    public RecurringEventSeries create(CreateRecurringEventSeriesCommand command) {
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        CalendarRecurringEventSeriesEntity entity = new CalendarRecurringEventSeriesEntity();
        entity.setSpaceId(command.spaceId());
        entity.setTitleEncrypted(encryptor.encrypt(command.title()));
        entity.setDescriptionEncrypted(encryptOrNull(encryptor, command.description()));
        entity.setLocationEncrypted(encryptOrNull(encryptor, command.location()));
        apply(entity, command.allDay(), command.startTime(), command.endTime(), command.durationDays(),
            command.color(), command.intervalType(), command.intervalCount(), command.anchorDate(), command.endDate());
        entity.setStartsOn(command.startsOn());
        entity.setCreatedBy(command.createdBy());
        CalendarRecurringEventSeriesEntity saved = series.saveAndFlush(entity);
        replaceParticipants(saved.getId(), command.participantIds());
        return toDomain(saved, List.copyOf(command.participantIds()));
    }

    @Override
    @Transactional
    public RecurringEventSeries update(UpdateRecurringEventSeriesCommand command) {
        CalendarRecurringEventSeriesEntity entity = series.findById(command.seriesId())
            .orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        TextEncryptor encryptor = encryptorFactory.forSpace(entity.getSpaceId());
        entity.setTitleEncrypted(encryptor.encrypt(command.title()));
        entity.setDescriptionEncrypted(encryptOrNull(encryptor, command.description()));
        entity.setLocationEncrypted(encryptOrNull(encryptor, command.location()));
        apply(entity, command.allDay(), command.startTime(), command.endTime(), command.durationDays(),
            command.color(), command.intervalType(), command.intervalCount(), command.anchorDate(), command.endDate());
        CalendarRecurringEventSeriesEntity saved = series.saveAndFlush(entity);
        replaceParticipants(saved.getId(), command.participantIds());
        return toDomain(saved, List.copyOf(command.participantIds()));
    }

    @Override
    @Transactional
    public void endOn(UUID seriesId, LocalDate lastDay) {
        CalendarRecurringEventSeriesEntity entity = series.findById(seriesId)
            .orElseThrow(CalendarException.RecurringEventSeriesNotFound::new);
        entity.setEndDate(lastDay);
        series.saveAndFlush(entity);
    }

    @Override
    @Transactional
    public void delete(UUID seriesId) {
        // Exclusions, participants and detached instances go with it via ON DELETE CASCADE.
        series.deleteById(seriesId);
    }

    private static void apply(CalendarRecurringEventSeriesEntity entity, boolean allDay,
                              java.time.LocalTime startTime, java.time.LocalTime endTime, int durationDays,
                              String color, com.nido.api.calendar.domain.model.RecurrenceInterval intervalType,
                              int intervalCount, java.time.LocalDate anchorDate, java.time.LocalDate endDate) {
        entity.setAllDay(allDay);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setDurationDays(durationDays);
        entity.setColor(color);
        entity.setIntervalType(intervalType);
        entity.setIntervalCount(intervalCount);
        entity.setAnchorDate(anchorDate);
        entity.setEndDate(endDate);
    }

    private void replaceParticipants(UUID seriesId, List<UUID> userIds) {
        participants.deleteBySeriesId(seriesId);
        participants.flush();
        for (UUID userId : new LinkedHashSet<>(userIds)) {
            CalendarRecurringEventSeriesParticipantEntity row = new CalendarRecurringEventSeriesParticipantEntity();
            row.setSeriesId(seriesId);
            row.setUserId(userId);
            participants.save(row);
        }
    }

    private List<UUID> participantIdsOf(UUID seriesId) {
        return participants.findBySeriesId(seriesId).stream()
            .map(CalendarRecurringEventSeriesParticipantEntity::getUserId).toList();
    }

    private static String encryptOrNull(TextEncryptor encryptor, String value) {
        return value == null ? null : encryptor.encrypt(value);
    }

    private static String decryptOrNull(TextEncryptor encryptor, String value) {
        return value == null ? null : encryptor.decrypt(value);
    }

    private RecurringEventSeries toDomain(CalendarRecurringEventSeriesEntity e, List<UUID> participantIds) {
        TextEncryptor encryptor = encryptorFactory.forSpace(e.getSpaceId());
        return new RecurringEventSeries(
            e.getId(), e.getSpaceId(),
            encryptor.decrypt(e.getTitleEncrypted()),
            decryptOrNull(encryptor, e.getDescriptionEncrypted()),
            decryptOrNull(encryptor, e.getLocationEncrypted()),
            e.isAllDay(), e.getStartTime(), e.getEndTime(), e.getDurationDays(), e.getColor(),
            e.getIntervalType(), e.getIntervalCount(), e.getAnchorDate(), e.getEndDate(), e.getStartsOn(),
            participantIds, e.getCreatedBy(), e.getCreatedAt());
    }
}
