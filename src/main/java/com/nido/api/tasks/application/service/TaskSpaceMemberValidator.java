package com.nido.api.tasks.application.service;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.tasks.domain.model.TaskException;

import java.util.UUID;

/**
 * Confirms a rotationMemberId a caller submitted is an actual member of the space in
 * question. Nothing else constrains that id to the caller's own space, so every write that
 * accepts a rotation list from the request body needs this check — without it, an unknown or
 * foreign UUID reaches the database as a foreign key value and blows up with a raw
 * DataIntegrityViolationException (500) instead of a clean 4xx.
 *
 * <p>Named with a {@code Task} prefix (mirroring finance's identically-purposed
 * {@code SpaceMemberValidator}) rather than sharing that simple name: Spring's default
 * component-scan bean naming ignores package, so two classes both named
 * {@code SpaceMemberValidator} collide as the same bean id and fail context startup.
 */
@ApplicationService
public class TaskSpaceMemberValidator {

    private final SpaceMembershipPort spaceMembershipPort;

    public TaskSpaceMemberValidator(SpaceMembershipPort spaceMembershipPort) {
        this.spaceMembershipPort = spaceMembershipPort;
    }

    public void ensureMember(UUID spaceId, UUID memberId) {
        spaceMembershipPort.find(spaceId, memberId).orElseThrow(TaskException.MemberNotInSpace::new);
    }
}
