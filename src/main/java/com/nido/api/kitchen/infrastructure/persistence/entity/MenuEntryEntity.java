package com.nido.api.kitchen.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "kitchen_menu_entries")
@Getter
@Setter
@NoArgsConstructor
public class MenuEntryEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(nullable = false)
    private int portions;

    @Column(nullable = false)
    private int position;
}
