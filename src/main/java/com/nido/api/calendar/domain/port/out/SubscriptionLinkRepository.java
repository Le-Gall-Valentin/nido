package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.SubscriptionLink;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SubscriptionLinkRepository {

    SubscriptionLink create(UUID spaceId, UUID userId, String label, Set<CalendarSourceType> sources, boolean onlyMine,
                            String tokenHash);

    List<SubscriptionLink> findBySpaceAndUser(UUID spaceId, UUID userId);

    Optional<SubscriptionLink> findByTokenHash(String tokenHash);

    long countBySpaceAndUser(UUID spaceId, UUID userId);

    /** Deletes the address if {@code userId} gave it for {@code spaceId}, and says whether it did; its label is never opened. */
    boolean deleteOwned(UUID linkId, UUID spaceId, UUID userId);

    /** Every address a member gave for a space: what their leaving it takes away. */
    void deleteBySpaceAndUser(UUID spaceId, UUID userId);

    void markRead(UUID linkId, Instant at);
}
