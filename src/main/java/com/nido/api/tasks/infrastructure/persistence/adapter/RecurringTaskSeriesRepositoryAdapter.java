package com.nido.api.tasks.infrastructure.persistence.adapter;

import com.nido.api.tasks.domain.model.CreateRecurringTaskSeriesCommand;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.TaskException;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesMemberEntity;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesSubtaskTemplateEntity;
import com.nido.api.tasks.infrastructure.persistence.repository.RecurringTaskSeriesJpaRepository;
import com.nido.api.tasks.infrastructure.persistence.repository.RecurringTaskSeriesMemberJpaRepository;
import com.nido.api.tasks.infrastructure.persistence.repository.RecurringTaskSeriesSubtaskTemplateJpaRepository;
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

    public RecurringTaskSeriesRepositoryAdapter(RecurringTaskSeriesJpaRepository series,
                                                 RecurringTaskSeriesMemberJpaRepository members,
                                                 RecurringTaskSeriesSubtaskTemplateJpaRepository subtaskTemplates) {
        this.series = series;
        this.members = members;
        this.subtaskTemplates = subtaskTemplates;
    }

    @Override
    public Optional<RecurringTaskSeries> findById(UUID seriesId) {
        return series.findById(seriesId).map(this::toDomain);
    }

    @Override
    @Transactional
    public RecurringTaskSeries create(CreateRecurringTaskSeriesCommand command) {
        RecurringTaskSeriesEntity e = new RecurringTaskSeriesEntity();
        e.setSpaceId(command.spaceId());
        e.setTitle(command.title());
        e.setPriority(command.priority());
        e.setIntervalType(command.intervalType());
        e.setIntervalCount(command.intervalCount());
        e.setAnchorDate(command.anchorDate());
        e.setOccurrenceCount(0);
        e.setCurrentRotationIndex(0);
        RecurringTaskSeriesEntity saved = series.saveAndFlush(e);
        saveMembersAndTemplates(saved.getId(), command.rotationMemberIds(), command.subtaskTemplates());
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

    private void saveMembersAndTemplates(UUID seriesId, List<UUID> rotationMemberIds, List<String> subtaskTemplateTexts) {
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
            te.setText(subtaskTemplateTexts.get(i));
            subtaskTemplates.save(te);
        }
        members.flush();
        subtaskTemplates.flush();
    }

    private RecurringTaskSeries toDomain(RecurringTaskSeriesEntity e) {
        List<UUID> rotationMemberIds = members.findBySeriesIdOrderByPositionAsc(e.getId()).stream()
            .map(RecurringTaskSeriesMemberEntity::getUserId).toList();
        List<String> templates = subtaskTemplates.findBySeriesIdOrderByPositionAsc(e.getId()).stream()
            .map(RecurringTaskSeriesSubtaskTemplateEntity::getText).toList();
        return new RecurringTaskSeries(e.getId(), e.getSpaceId(), e.getTitle(), e.getPriority(), templates,
            e.getIntervalType(), e.getIntervalCount(), e.getAnchorDate(), e.getOccurrenceCount(),
            rotationMemberIds, e.getCurrentRotationIndex());
    }
}
