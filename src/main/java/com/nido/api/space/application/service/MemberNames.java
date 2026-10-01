package com.nido.api.space.application.service;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.port.out.MemberProfilePort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The usernames of a set of accounts, in one call across the bridge to identity — no N+1 for the lists
 * that say who invited whom. A missing id is skipped, and an account that can no longer be named (an
 * anonymised one keeps its row but loses its name) is simply absent: callers show it as deleted.
 */
@ApplicationService
public class MemberNames {

    private final MemberProfilePort memberProfilePort;

    public MemberNames(MemberProfilePort memberProfilePort) {
        this.memberProfilePort = memberProfilePort;
    }

    public Map<UUID, String> byId(Collection<UUID> userIds) {
        List<UUID> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberProfilePort.findByIds(ids).stream()
            .filter(profile -> profile.username() != null)
            .collect(Collectors.toMap(MemberProfile::userId, MemberProfile::username));
    }
}
