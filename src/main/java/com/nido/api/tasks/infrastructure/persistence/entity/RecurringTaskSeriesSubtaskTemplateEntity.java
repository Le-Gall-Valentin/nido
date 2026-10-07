package com.nido.api.tasks.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "recurring_task_series_subtask_templates")
@Getter
@Setter
@NoArgsConstructor
public class RecurringTaskSeriesSubtaskTemplateEntity extends AssignedUuidEntity {

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(nullable = false)
    private int position;

    @Column(name = "text_encrypted", nullable = false)
    private String textEncrypted;
}
