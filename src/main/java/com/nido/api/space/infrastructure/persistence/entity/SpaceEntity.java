package com.nido.api.space.infrastructure.persistence.entity;

import com.nido.api.space.domain.model.SpaceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
