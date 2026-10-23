package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.space.domain.model.Addressee;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A gesture in the finances of a space, and the members who could hear about it.
 *
 * @param memberNames the space's current members who can still be named, its author included, in the space's order
 */
public record Audience(Gesture gesture, Map<UUID, String> memberNames) {

    public Audience {
        Objects.requireNonNull(gesture, "gesture");
        memberNames = Collections.unmodifiableMap(new LinkedHashMap<>(memberNames));
    }

    /** Those of {@code ids} who can be told: named members of the space, its author left out, each once, in that order. */
    public List<Addressee> readers(Collection<UUID> ids) {
        return new LinkedHashSet<>(ids).stream()
            .filter(id -> !id.equals(gesture.actorId()) && memberNames.containsKey(id))
            .map(id -> new Addressee(id, memberNames.get(id)))
            .toList();
    }

    /** Every member who can be told, but the author. */
    public List<Addressee> everyoneButTheActor() {
        return readers(memberNames.keySet());
    }

    /** The name a mail gives a member; null for someone who left the space or can no longer be named. */
    public String nameOf(UUID id) {
        return id == null ? null : memberNames.get(id);
    }
}
