package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What every source reads with. {@code today} is the space's date, never the server's.
 */
public record DashboardContext(SpaceMembership caller, LocalDate today, SpaceType spaceType) {

    public UUID callerId() {
        return caller.userId();
    }

    public boolean isShared() {
        return spaceType == SpaceType.SHARED;
    }

    /**
     * Whether a task calls for the caller's action: assigned to them or to nobody — an unassigned chore
     * is anyone's. In a shared space somebody else's task does not; in a personal space every task is
     * the owner's.
     */
    public boolean isMine(List<UUID> assigneeIds) {
        return !isShared() || assigneeIds.isEmpty() || assigneeIds.contains(callerId());
    }
}
