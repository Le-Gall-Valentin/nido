package com.nido.api.shopping.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.shared.model.MeasurementUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shopping_items")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingItemEntity extends AssignedUuidEntity {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    public static final SealedColumn NAME = SealedColumn.ofSpace("shopping_items", "name_encrypted").withClearColumn("name");

    @Column(name = "name_encrypted", nullable = false)
    private String nameEncrypted;

    @Column(precision = 10, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MeasurementUnit unit;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private int position;
}
