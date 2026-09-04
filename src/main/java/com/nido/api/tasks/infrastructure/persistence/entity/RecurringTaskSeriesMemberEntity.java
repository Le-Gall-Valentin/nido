package com.nido.api.tasks.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "recurring_task_series_members")
@Getter
@Setter
@NoArgsConstructor
public class RecurringTaskSeriesMemberEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(nullable = false)
    private int position;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
