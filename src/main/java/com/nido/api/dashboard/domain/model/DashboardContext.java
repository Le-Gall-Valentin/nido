package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * What every source reads with. {@code today} is the space's date, never the server's;
 * {@code callerEmail} is the address invitations are sent to.
 */
public record DashboardContext(SpaceMembership caller, String callerEmail, LocalDate today, SpaceType spaceType) {

    public UUID callerId() {
        return caller.userId();
    }

    public boolean isShared() {
        return spaceType == SpaceType.SHARED;
    }
}
