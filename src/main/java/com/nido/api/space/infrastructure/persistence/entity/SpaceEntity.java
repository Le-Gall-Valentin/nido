package com.nido.api.space.infrastructure.persistence.entity;

import com.nido.api.space.domain.model.SpaceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "spaces")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SpaceEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpaceType type;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 280)
    private String description;

    @Column(nullable = false, length = 7)
    private String accent;

    @Column(nullable = false, length = 8)
    private String glyph;

    @Column(name = "personal_owner_id")
    private UUID personalOwnerId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "encryption_salt", nullable = false, updatable = false, length = 32)
    private String encryptionSalt;

    @Column(nullable = false, length = 64)
    private String timezone;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA allows exactly one of these per entity, so both defaults live here.
     *
     * <p>Every space needs a stable per-space encryption salt before Finance can derive a key for
     * it — generated here rather than at every call site so no caller can forget it.
     *
     * <p>The timezone column has a database default, which an INSERT naming the column defeats:
     * Hibernate writes the field as it stands, and a null field becomes an explicit NULL against a
     * NOT NULL constraint. Europe/Paris rather than the server's own zone — the server runs in UTC,
     * which is nobody's household — and rather than throwing, so a caller with no browser to ask
     * still gets a space. Callers that do know pass the creator's zone and never reach this.
     */
    @PrePersist
    void applyDefaults() {
        if (timezone == null) {
            timezone = "Europe/Paris";
        }
        if (encryptionSalt == null) {
            byte[] bytes = new byte[16];
            new SecureRandom().nextBytes(bytes);
            encryptionSalt = HexFormat.of().formatHex(bytes);
        }
    }
}
