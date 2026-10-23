package com.nido.api.space.domain.port.out;

import java.util.UUID;

/**
 * What another module does when someone stops being a member of a space — they left, were removed, or their account was
 * deleted. Called in the transaction that removes the membership. A space deleted whole needs none: its rows go with it.
 */
public interface SpaceMemberDepartureHook {
    void departed(UUID spaceId, UUID userId);
}
