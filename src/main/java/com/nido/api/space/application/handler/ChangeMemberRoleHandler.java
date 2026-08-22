package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ChangeMemberRoleUseCase;
import com.nido.api.space.domain.model.ChangeMemberRoleCommand;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class ChangeMemberRoleHandler implements ChangeMemberRoleUseCase {

    private final SpaceMembershipPort spaceMembershipPort;

    public ChangeMemberRoleHandler(SpaceMembershipPort spaceMembershipPort) {
        this.spaceMembershipPort = spaceMembershipPort;
    }

    @Override
    @Transactional
    public void change(ChangeMemberRoleCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanManageSpace();
        if (command.targetUserId().equals(caller.userId())) {
            throw new SpaceException.SelfManagementForbidden();
        }
        if (command.newRole() == SpaceRole.OWNER) {
            throw new SpaceException.OwnerRoleNotAssignable();
        }
        SpaceMembership target = spaceMembershipPort.find(command.spaceId(), command.targetUserId())
            .orElseThrow(SpaceException.MemberNotFound::new);
        if (target.isOwner()) {
            throw new SpaceException.OwnerMembershipProtected();
        }
        if (target.role() == command.newRole()) {
            throw new SpaceException.RoleAlreadyAssigned();
        }
        spaceMembershipPort.changeRole(target.id(), command.newRole());
    }
}
