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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Every space needs a stable per-space encryption salt before Finance can derive a key
    // for it — generated here rather than at every call site so no caller can forget it.
    @PrePersist
    void generateEncryptionSaltIfMissing() {
        if (encryptionSalt == null) {
            byte[] bytes = new byte[16];
            new SecureRandom().nextBytes(bytes);
            encryptionSalt = HexFormat.of().formatHex(bytes);
        }
    }
}
