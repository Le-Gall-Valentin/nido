package com.nido.api.space.domain.port.out;

import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceSummaryView;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceRepository {
    Optional<Space> findById(UUID spaceId);
    Optional<Space> findPersonalOwnedBy(UUID userId);
    List<SpaceSummaryView> findMySpaces(UUID userId);
    long countMembers(UUID spaceId);
    /** Lecture par lot, pour enrichir une liste sans lecture par élément. */
    List<Space> findByIds(Collection<UUID> spaceIds);
}
