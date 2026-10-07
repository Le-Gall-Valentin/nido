package com.nido.api.infrastructure.persistence.entity;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * The base of every entity holding sealed values: its id is drawn by the application, the first time it is asked, so
 * a value can be sealed with the row it belongs to before the row is inserted. A generated id only exists once
 * Hibernate persists the entity, too late; and an id set on an {@code @UuidGenerator} entity makes Spring Data merge
 * instead of insert, which Hibernate 7 refuses with {@code ObjectOptimisticLockingFailureException}. {@link #isNew()}
 * tells Spring Data which: new until persisted or loaded.
 *
 * <p>Drawn when asked rather than in the constructor, through which Hibernate also builds every row it loads — before
 * setting the id the row has.
 */
@MappedSuperclass
public abstract class AssignedUuidEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean stored;

    @Override
    public UUID getId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        return id;
    }

    @Override
    public boolean isNew() {
        return !stored;
    }

    /** An entity inserted before anyone asked its id gets one now: Hibernate reads the field, not this getter. */
    @PrePersist
    void drawIdIfNone() {
        getId();
    }

    @PostPersist
    @PostLoad
    void markStored() {
        stored = true;
    }
}
