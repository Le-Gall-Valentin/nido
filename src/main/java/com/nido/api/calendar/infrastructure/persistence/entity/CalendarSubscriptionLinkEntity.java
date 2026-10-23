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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendar_subscription_links")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CalendarSubscriptionLinkEntity extends AssignedUuidEntity {

    public static final SealedColumn LABEL = SealedColumn.ofSpace("calendar_subscription_links", "label_encrypted");

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "label_encrypted", nullable = false)
    private String labelEncrypted;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** The sources it writes, as "EVENT,TASK". */
    @Column(nullable = false, length = 60)
    private String sources;

    @Column(name = "only_mine", nullable = false)
    private boolean onlyMine;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_read_at")
    private Instant lastReadAt;
}
