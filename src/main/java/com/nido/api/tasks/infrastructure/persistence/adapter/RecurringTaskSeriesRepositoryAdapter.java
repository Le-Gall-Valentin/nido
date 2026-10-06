package com.nido.api.tasks.infrastructure.persistence.adapter;

import com.nido.api.infrastructure.config.SpaceEncryptorFactory;
import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.RecurringTaskSeriesSchedule;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.model.UpdateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesMemberEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesSubtaskTemplateEntity;
import com.nido.api.tasks.infrastructure.persistence.repository.RecurringTaskSeriesJpaRepository;
import com.nido.api.tasks.infrastructure.persistence.repository.RecurringTaskSeriesMemberJpaRepository;
import com.nido.api.tasks.infrastructure.persistence.repository.RecurringTaskSeriesSubtaskTemplateJpaRepository;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RecurringTaskSeriesRepositoryAdapter implements RecurringTaskSeriesRepository {

    private final RecurringTaskSeriesJpaRepository series;
    private final RecurringTaskSeriesMemberJpaRepository members;
    private final RecurringTaskSeriesSubtaskTemplateJpaRepository subtaskTemplates;
    private final SpaceEncryptorFactory encryptors;

    public RecurringTaskSeriesRepositoryAdapter(RecurringTaskSeriesJpaRepository series,
                                                 RecurringTaskSeriesMemberJpaRepository members,
                                                 RecurringTaskSeriesSubtaskTemplateJpaRepository subtaskTemplates,
                                                 SpaceEncryptorFactory encryptors) {
        this.series = series;
        this.members = members;
        this.subtaskTemplates = subtaskTemplates;
        this.encryptors = encryptors;
    }

    @Override
    public Optional<RecurringTaskSeries> findById(UUID seriesId) {
        return series.findById(seriesId).map(this::toDomain);
    }

    @Override
    public List<RecurringTaskSeries> findBySpaceId(UUID spaceId) {
        return series.findBySpaceId(spaceId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<RecurringTaskSeriesSchedule> findSchedulesBySpaceId(UUID spaceId) {
        return series.findSchedulesBySpaceId(spaceId);
    }

    @Override
    @Transactional
    public RecurringTaskSeries create(CreateRecurringTaskSeriesCommand command) {
        TextEncryptor encryptor = encryptors.forSpace(command.spaceId());
        RecurringTaskSeriesEntity e = new RecurringTaskSeriesEntity();
        e.setSpaceId(command.spaceId());
        e.setTitleEncrypted(encryptor.encrypt(command.title()));
        e.setPriority(command.priority());
        e.setIntervalType(command.intervalType());
        e.setIntervalCount(command.intervalCount());
        e.setLeadIntervalType(command.leadIntervalType());
        e.setLeadIntervalCount(command.leadIntervalCount());
        e.setAnchorDate(command.anchorDate());
        e.setEndDate(command.endDate());
        e.setOccurrenceCount(0);
        e.setCurrentRotationIndex(0);
        e.setCreatedBy(command.creatorUserId());
        RecurringTaskSeriesEntity saved = series.saveAndFlush(e);
        saveMembersAndTemplates(saved.getId(), encryptor, command.rotationMemberIds(), command.subtaskTemplates());
        return findById(saved.getId()).orElseThrow(TaskException.TaskNotFound::new);
    }

    @Override
    @Transactional
    public RecurringTaskSeries update(UpdateRecurringTaskSeriesCommand command) {
        RecurringTaskSeriesEntity e = series.findById(command.seriesId()).orElseThrow(TaskException.RecurringSeriesNotFound::new);
        TextEncryptor encryptor = encryptors.forSpace(e.getSpaceId());
        e.setTitleEncrypted(encryptor.encrypt(command.title()));
        e.setPriority(command.priority());
        e.setIntervalType(command.intervalType());
        e.setIntervalCount(command.intervalCount());
        e.setLeadIntervalType(command.leadIntervalType());
        e.setLeadIntervalCount(command.leadIntervalCount());
        e.setAnchorDate(command.anchorDate());
        e.setEndDate(command.endDate());
        RecurringTaskSeriesEntity saved = series.saveAndFlush(e);
        members.deleteBySeriesId(saved.getId());
        members.flush();
        subtaskTemplates.deleteBySeriesId(saved.getId());
        subtaskTemplates.flush();
        saveMembersAndTemplates(saved.getId(), encryptor, command.rotationMemberIds(), command.subtaskTemplates());
        return findById(saved.getId()).orElseThrow(TaskException.TaskNotFound::new);
    }

    @Override
    @Transactional
    public RecurringTaskSeries advance(UUID seriesId, int nextRotationIndex, int nextOccurrenceCount) {
        RecurringTaskSeriesEntity e = series.findById(seriesId).orElseThrow(TaskException.TaskNotFound::new);
        e.setCurrentRotationIndex(nextRotationIndex);
        e.setOccurrenceCount(nextOccurrenceCount);
        series.saveAndFlush(e);
        return findById(seriesId).orElseThrow(TaskException.TaskNotFound::new);
    }

    @Override
    public void deleteById(UUID seriesId) {
        series.deleteById(seriesId);
        series.flush();
    }

    @Override
    @Transactional
    public void lockForMaterialization(UUID spaceId) {
        series.lockMaterialization("tasks-materialize|" + spaceId);
    }

    private void saveMembersAndTemplates(UUID seriesId, TextEncryptor encryptor, List<UUID> rotationMemberIds,
                                         List<String> subtaskTemplateTexts) {
        for (int i = 0; i < rotationMemberIds.size(); i++) {
            RecurringTaskSeriesMemberEntity me = new RecurringTaskSeriesMemberEntity();
            me.setSeriesId(seriesId);
            me.setPosition(i);
            me.setUserId(rotationMemberIds.get(i));
            members.save(me);
        }
        for (int i = 0; i < subtaskTemplateTexts.size(); i++) {
            RecurringTaskSeriesSubtaskTemplateEntity te = new RecurringTaskSeriesSubtaskTemplateEntity();
            te.setSeriesId(seriesId);
            te.setPosition(i);
            te.setTextEncrypted(encryptor.encrypt(subtaskTemplateTexts.get(i)));
            subtaskTemplates.save(te);
        }
        members.flush();
        subtaskTemplates.flush();
    }

    private RecurringTaskSeries toDomain(RecurringTaskSeriesEntity e) {
        TextEncryptor encryptor = encryptors.forSpace(e.getSpaceId());
        List<UUID> rotationMemberIds = members.findBySeriesIdOrderByPositionAsc(e.getId()).stream()
            .map(RecurringTaskSeriesMemberEntity::getUserId).toList();
        List<String> templates = subtaskTemplates.findBySeriesIdOrderByPositionAsc(e.getId()).stream()
            .map(t -> encryptor.decrypt(t.getTextEncrypted())).toList();
        return new RecurringTaskSeries(e.getId(), e.getSpaceId(), encryptor.decrypt(e.getTitleEncrypted()), e.getPriority(), templates,
            e.getIntervalType(), e.getIntervalCount(), e.getLeadIntervalType(), e.getLeadIntervalCount(),
            e.getAnchorDate(), e.getEndDate(), e.getOccurrenceCount(), rotationMemberIds, e.getCurrentRotationIndex(),
            e.getCreatedBy());
    }
}
