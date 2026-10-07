package com.nido.api.tasks.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
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

    public static final SealedColumn TEXT = SealedColumn.throughParent("recurring_task_series_subtask_templates", "text_encrypted",
        "series_id", "recurring_task_series").withClearColumn("text");

    @Column(name = "text_encrypted", nullable = false)
    private String textEncrypted;
}
