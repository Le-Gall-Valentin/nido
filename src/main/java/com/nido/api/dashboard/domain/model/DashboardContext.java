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

    /**
     * Whether a task calls for the caller's action: assigned to them or to nobody — an unassigned chore
     * is anyone's. In a shared space somebody else's task does not; in a personal space every task is
     * the owner's.
     */
    public boolean isMine(TaskItem task) {
        return !isShared() || task.assigneeIds().isEmpty() || task.assigneeIds().contains(callerId());
    }
}
