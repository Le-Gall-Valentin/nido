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
@Table(name = "shopping_items")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingItemEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "quantity_label", length = 40)
    private String quantityLabel;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private int position;
}
