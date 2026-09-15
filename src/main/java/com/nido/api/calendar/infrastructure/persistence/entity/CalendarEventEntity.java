package com.nido.api.calendar.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "calendar_events")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CalendarEventEntity {

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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 30)
    private String color;

    /** Set together with recurringOriginalDate, and only for a detached occurrence. */
    @Column(name = "recurring_series_id")
    private UUID recurringSeriesId;

    /** The slot this row replaces — never where the event now sits. */
    @Column(name = "recurring_original_date")
    private LocalDate recurringOriginalDate;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
