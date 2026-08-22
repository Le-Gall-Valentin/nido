package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.RemoveMemberUseCase;
import com.nido.api.space.domain.model.RemoveMemberCommand;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class RemoveMemberHandler implements RemoveMemberUseCase {

    private static final Logger log = LoggerFactory.getLogger(RemoveMemberHandler.class);

    private final SpaceMembershipPort spaceMembershipPort;

    public RemoveMemberHandler(SpaceMembershipPort spaceMembershipPort) {
        this.spaceMembershipPort = spaceMembershipPort;
    }

    @Override
    @Transactional
    public void remove(RemoveMemberCommand command, SpaceMembership caller) {
        caller.ensureCanManageSpace();
        if (command.targetUserId().equals(caller.userId())) {
            throw new SpaceException.SelfManagementForbidden();
        }
        SpaceMembership target = spaceMembershipPort.find(command.spaceId(), command.targetUserId())
            .orElseThrow(SpaceException.MemberNotFound::new);
        if (target.isOwner()) {
            throw new SpaceException.OwnerMembershipProtected();
        }
        spaceMembershipPort.remove(target.id());
        log.info("User {} removed from space {} by {}",
            command.targetUserId(), command.spaceId(), caller.userId());
    }
}
