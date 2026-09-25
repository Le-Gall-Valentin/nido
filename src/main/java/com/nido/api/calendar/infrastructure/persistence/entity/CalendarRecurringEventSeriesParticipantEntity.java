package com.nido.api.calendar.infrastructure.persistence.entity;

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
@Table(name = "calendar_recurring_event_series_participants")
@Getter
@Setter
@NoArgsConstructor
public class CalendarRecurringEventSeriesParticipantEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
