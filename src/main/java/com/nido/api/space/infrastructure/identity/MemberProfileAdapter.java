package com.nido.api.space.infrastructure.identity;

import com.nido.api.identity.application.port.in.FindUserUseCase;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class MemberProfileAdapter implements MemberProfilePort {

    private final FindUserUseCase findUserUseCase;

    public MemberProfileAdapter(FindUserUseCase findUserUseCase) {
        this.findUserUseCase = findUserUseCase;
    }

    @Override
    public List<MemberProfile> findByIds(Collection<UUID> userIds) {
        return findUserUseCase.findByIds(userIds).stream()
            .map(u -> new MemberProfile(u.id(), u.username(), u.email()))
            .toList();
    }
}
