package com.nido.api.kitchen.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kitchen_recipes")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RecipeEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    public static final SealedColumn NAME = SealedColumn.ofSpace("kitchen_recipes", "name_encrypted").withClearColumn("name");

    @Column(name = "name_encrypted", nullable = false)
    private String nameEncrypted;

    public static final SealedColumn DESCRIPTION = SealedColumn.ofSpace("kitchen_recipes", "description_encrypted").withClearColumn("description");

    @Column(name = "description_encrypted")
    private String descriptionEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipeCategory category;

    @Column(nullable = false)
    private int minutes;

    @Column(name = "reference_portions", nullable = false)
    private int referencePortions;

    @Column(nullable = false)
    private boolean favorite;

    public static final SealedColumn NOTE = SealedColumn.ofSpace("kitchen_recipes", "note_encrypted").withClearColumn("note");

    @Column(name = "note_encrypted")
    private String noteEncrypted;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
