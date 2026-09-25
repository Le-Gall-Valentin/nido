package com.nido.api.calendar.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

/** One cancelled occurrence. A cancellation is an absence, so there is nothing else to store. */
@Entity
@Table(name = "calendar_event_exclusions")
@Getter
@Setter
@NoArgsConstructor
public class CalendarEventExclusionEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "original_date", nullable = false)
    private LocalDate originalDate;
}
