package com.nido.api.space.application.handler;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ListSpaceMembersUseCase;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.SpaceMemberView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationService
public class ListSpaceMembersHandler implements ListSpaceMembersUseCase {

    private final SpaceMembershipPort spaceMembershipPort;
    private final MemberProfilePort memberProfilePort;

    public ListSpaceMembersHandler(SpaceMembershipPort spaceMembershipPort,
                                   MemberProfilePort memberProfilePort) {
        this.spaceMembershipPort = spaceMembershipPort;
        this.memberProfilePort = memberProfilePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceMemberView> list(SpaceMembership caller) {
        List<SpaceMembership> memberships = spaceMembershipPort.findMemberships(caller.spaceId());
        List<UUID> userIds = memberships.stream().map(SpaceMembership::userId).toList();
        // un seul appel pour tous les profils : pas de N+1 sur le pont vers identity
        Map<UUID, MemberProfile> profiles = memberProfilePort.findByIds(userIds).stream()
            .collect(Collectors.toMap(MemberProfile::userId, Function.identity()));
        return memberships.stream()
            .map(m -> {
                MemberProfile profile = profiles.get(m.userId());
                return new SpaceMemberView(m.userId(),
                    profile == null ? null : profile.username(),
                    profile == null ? null : profile.email(),
                    m.role(), m.joinedAt());
            })
            .toList();
    }
}
