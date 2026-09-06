package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;

import java.util.UUID;

/**
 * Confirms a memberId a caller submitted (a payer, a contributor, a settlement party, a
 * savings contributor) is an actual member of the space in question. None of these ids are
 * otherwise constrained to the caller's own space, so every finance write that accepts one
 * from the request body needs this check.
 */
@ApplicationService
public class SpaceMemberValidator {

    private final SpaceMembershipPort spaceMembershipPort;

    public SpaceMemberValidator(SpaceMembershipPort spaceMembershipPort) {
        this.spaceMembershipPort = spaceMembershipPort;
    }

    public void ensureMember(UUID spaceId, UUID memberId) {
        spaceMembershipPort.find(spaceId, memberId).orElseThrow(FinanceException.MemberNotInSpace::new);
    }
}
