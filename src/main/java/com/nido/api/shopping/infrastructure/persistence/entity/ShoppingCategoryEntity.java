package com.nido.api.shopping.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "shopping_categories")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingCategoryEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "name_encrypted", nullable = false)
    private String nameEncrypted;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean fallback;
}
