package com.nido.api.infrastructure.persistence.entity;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * The base of every entity holding sealed values: its id is drawn when the object is built, so a value can be sealed
 * with the row it belongs to before the row is inserted. A generated id only exists once Hibernate persists the
 * entity, too late; and an id set on an {@code @UuidGenerator} entity makes Spring Data merge instead of insert, which
 * Hibernate 7 refuses with {@code ObjectOptimisticLockingFailureException}. {@link #isNew()} tells Spring Data which:
 * new until persisted or loaded.
 */
@MappedSuperclass
public abstract class AssignedUuidEntity implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Transient
    private boolean stored;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return !stored;
    }

    @PostPersist
    @PostLoad
    void markStored() {
        stored = true;
    }
}
