package com.nido.api.shopping.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "shopping_categories")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingCategoryEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean fallback;
}
