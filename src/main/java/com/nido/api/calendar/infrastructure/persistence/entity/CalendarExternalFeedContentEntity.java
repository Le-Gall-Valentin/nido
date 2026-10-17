package com.nido.api.calendar.infrastructure.persistence.entity;

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
@Table(name = "calendar_external_feed_contents")
@Getter
@Setter
@NoArgsConstructor
public class CalendarExternalFeedContentEntity extends AssignedUuidEntity {

    /** Sealed with its own row's id, its space read through the calendar it belongs to. */
    public static final SealedColumn CONTENT = SealedColumn.throughParent(
        "calendar_external_feed_contents", "content_encrypted", "feed_id", "calendar_external_feeds");

    @Column(name = "feed_id", nullable = false)
    private UUID feedId;

    /** The calendar downloaded, gzipped then base64'd, then sealed: see PackedContent. */
    @Column(name = "content_encrypted", nullable = false)
    private String contentEncrypted;
}
