package com.nido.api.kitchen.infrastructure.persistence.entity;

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
@Table(name = "kitchen_recipe_ingredients")
@Getter
@Setter
@NoArgsConstructor
public class RecipeIngredientEntity extends AssignedUuidEntity {

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(nullable = false)
    private int position;

    public static final SealedColumn NAME = SealedColumn.throughParent("kitchen_recipe_ingredients", "name_encrypted", "recipe_id", "kitchen_recipes")
        .withClearColumn("name");

    @Column(name = "name_encrypted", nullable = false)
    private String nameEncrypted;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeasurementUnit unit;
}
