package com.nido.api.finance.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Id;
import java.util.UUID;

@Entity
@Table(name = "finance_categories")
@Getter
@Setter
@NoArgsConstructor
public class FinanceCategoryEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    /** Populated only when {@link #isDefault} is true — a system label, not user data. */
    @Column(length = 60)
    private String label;

    /** Populated only when {@link #isDefault} is false. */
    @Column(name = "label_encrypted")
    private String labelEncrypted;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false, length = 40)
    private String icon;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
}
