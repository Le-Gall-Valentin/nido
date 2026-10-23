package com.nido.api.space.application.service;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.port.out.SpaceMemberDepartureHook;

import java.util.List;
import java.util.UUID;

/** Tells every module that cares that a member is gone; the space module never learns which ones they are. */
@ApplicationService
public class SpaceDepartures {

    private final List<SpaceMemberDepartureHook> hooks;

    public SpaceDepartures(List<SpaceMemberDepartureHook> hooks) {
        this.hooks = hooks;
    }

    public void departed(UUID spaceId, UUID userId) {
        hooks.forEach(hook -> hook.departed(spaceId, userId));
    }
}
