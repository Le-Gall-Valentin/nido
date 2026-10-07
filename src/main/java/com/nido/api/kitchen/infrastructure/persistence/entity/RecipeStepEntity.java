package com.nido.api.kitchen.infrastructure.persistence.entity;

import com.nido.api.infrastructure.persistence.entity.AssignedUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "kitchen_recipe_steps")
@Getter
@Setter
@NoArgsConstructor
public class RecipeStepEntity extends AssignedUuidEntity {

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(nullable = false)
    private int position;

    @Column(name = "text_encrypted", nullable = false)
    private String textEncrypted;
}
