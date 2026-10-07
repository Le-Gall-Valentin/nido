package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesEntity;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesParticipantEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarRecurringEventSeriesJpaRepository;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarRecurringEventSeriesParticipantJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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
    private final SpaceSealers sealers;

    public RecurringEventSeriesRepositoryAdapter(
            CalendarRecurringEventSeriesJpaRepository series,
            CalendarRecurringEventSeriesParticipantJpaRepository participants,
            SpaceSealers sealers) {
        this.series = series;
        this.participants = participants;
        this.sealers = sealers;
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
        SpaceSealer sealer = sealers.forSpace(command.spaceId());
        CalendarRecurringEventSeriesEntity entity = new CalendarRecurringEventSeriesEntity();
        entity.setSpaceId(command.spaceId());
        entity.setTitleEncrypted(sealer.seal(CalendarRecurringEventSeriesEntity.TITLE, entity.getId(), command.title()));
        entity.setDescriptionEncrypted(sealer.sealNullable(CalendarRecurringEventSeriesEntity.DESCRIPTION, entity.getId(), command.description()));
        entity.setLocationEncrypted(sealer.sealNullable(CalendarRecurringEventSeriesEntity.LOCATION, entity.getId(), command.location()));
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
        SpaceSealer sealer = sealers.forSpace(entity.getSpaceId());
        entity.setTitleEncrypted(sealer.seal(CalendarRecurringEventSeriesEntity.TITLE, entity.getId(), command.title()));
        entity.setDescriptionEncrypted(sealer.sealNullable(CalendarRecurringEventSeriesEntity.DESCRIPTION, entity.getId(), command.description()));
        entity.setLocationEncrypted(sealer.sealNullable(CalendarRecurringEventSeriesEntity.LOCATION, entity.getId(), command.location()));
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
                              LocalTime startTime, LocalTime endTime, int durationDays,
                              String color, RecurrenceInterval intervalType,
                              int intervalCount, LocalDate anchorDate, LocalDate endDate) {
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

    private RecurringEventSeries toDomain(CalendarRecurringEventSeriesEntity e, List<UUID> participantIds) {
        SpaceSealer sealer = sealers.forSpace(e.getSpaceId());
        return new RecurringEventSeries(
            e.getId(), e.getSpaceId(),
            sealer.open(CalendarRecurringEventSeriesEntity.TITLE, e.getId(), e.getTitleEncrypted()),
            sealer.openNullable(CalendarRecurringEventSeriesEntity.DESCRIPTION, e.getId(), e.getDescriptionEncrypted()),
            sealer.openNullable(CalendarRecurringEventSeriesEntity.LOCATION, e.getId(), e.getLocationEncrypted()),
            e.isAllDay(), e.getStartTime(), e.getEndTime(), e.getDurationDays(), e.getColor(),
            e.getIntervalType(), e.getIntervalCount(), e.getAnchorDate(), e.getEndDate(), e.getStartsOn(),
            participantIds, e.getCreatedBy(), e.getCreatedAt());
    }
}
