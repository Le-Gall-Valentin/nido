package com.nido.api.tasks.infrastructure.persistence.entity;

import com.nido.api.tasks.domain.model.RecurrenceInterval;
import com.nido.api.tasks.domain.model.TaskPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recurring_task_series")
@Getter
@Setter
@NoArgsConstructor
public class RecurringTaskSeriesEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", nullable = false, length = 10)
    private RecurrenceInterval intervalType;

    @Column(name = "interval_count", nullable = false)
    private int intervalCount;

    @Column(name = "anchor_date", nullable = false)
    private LocalDate anchorDate;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "current_rotation_index", nullable = false)
    private int currentRotationIndex;
}
