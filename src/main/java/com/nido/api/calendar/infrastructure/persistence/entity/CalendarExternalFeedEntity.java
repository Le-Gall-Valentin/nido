package com.nido.api.calendar.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

// Only the columns changed are written: a rename and a refresh at once each keep what the other writes.
@DynamicUpdate
@Entity
@Table(name = "calendar_external_feeds")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CalendarExternalFeedEntity extends AssignedUuidEntity {

    public static final SealedColumn NAME = SealedColumn.ofSpace("calendar_external_feeds", "name_encrypted");
    public static final SealedColumn URL = SealedColumn.ofSpace("calendar_external_feeds", "url_encrypted");

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "name_encrypted", nullable = false)
    private String nameEncrypted;

    @Column(name = "url_encrypted", nullable = false)
    private String urlEncrypted;

    @Column(length = 30)
    private String color;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "content_fetched_at", nullable = false)
    private Instant contentFetchedAt;

    @Column(name = "last_attempt_at", nullable = false)
    private Instant lastAttemptAt;

    @Column(name = "last_error", length = 30)
    private String lastError;

    @Column(length = 200)
    private String etag;

    @Column(name = "last_modified", length = 100)
    private String lastModified;
}
