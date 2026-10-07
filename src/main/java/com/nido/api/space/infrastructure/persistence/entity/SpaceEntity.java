package com.nido.api.space.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.space.domain.model.SpaceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spaces")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SpaceEntity extends AssignedUuidEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpaceType type;

    public static final SealedColumn NAME = SealedColumn.ofSpaceItself("spaces", "name_encrypted").withClearColumn("name");

    @Column(name = "name_encrypted", nullable = false)
    private String nameEncrypted;

    public static final SealedColumn DESCRIPTION = SealedColumn.ofSpaceItself("spaces", "description_encrypted").withClearColumn("description");

    @Column(name = "description_encrypted")
    private String descriptionEncrypted;

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
     * No default for the encryption salt: a space's name is sealed with it before the row is
     * inserted, so the adapter draws it first (SpaceSalt) — a salt drawn here would come after a
     * name sealed without one, and the column refuses a row that has none.
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
    }
}
