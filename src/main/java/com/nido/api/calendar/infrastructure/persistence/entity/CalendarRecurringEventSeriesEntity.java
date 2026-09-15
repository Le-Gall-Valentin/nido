package com.nido.api.calendar.infrastructure.persistence.entity;

import com.nido.api.calendar.domain.model.RecurrenceInterval;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_recurring_event_series")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CalendarRecurringEventSeriesEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "title_encrypted", nullable = false)
    private String titleEncrypted;

    @Column(name = "description_encrypted")
    private String descriptionEncrypted;

    @Column(name = "location_encrypted")
    private String locationEncrypted;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /** Days past its start that each occurrence runs; 0 for a same-day event. */
    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", nullable = false, length = 10)
    private RecurrenceInterval intervalType;

    @Column(name = "interval_count", nullable = false)
    private int intervalCount;

    @Column(name = "anchor_date", nullable = false)
    private LocalDate anchorDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
