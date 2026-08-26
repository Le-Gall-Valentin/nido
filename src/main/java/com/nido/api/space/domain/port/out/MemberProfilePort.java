package com.nido.api.space.domain.port.out;

import com.nido.api.space.domain.model.MemberProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberProfilePort {
    List<MemberProfile> findByIds(Collection<UUID> userIds);

    Optional<MemberProfile> findByEmail(String email);
}
