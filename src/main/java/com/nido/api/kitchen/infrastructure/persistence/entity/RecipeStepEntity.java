package com.nido.api.kitchen.infrastructure.persistence.entity;

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
@Table(name = "kitchen_recipe_steps")
@Getter
@Setter
@NoArgsConstructor
public class RecipeStepEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 2000)
    private String text;
}
