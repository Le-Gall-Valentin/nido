package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.InvitationStatus;
import com.nido.api.authentication.application.port.in.AccountInvitationStatusQuery;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
public class AccountInvitationStatusHandler implements AccountInvitationStatusQuery {

    private final AccountInvitationRepository invitations;
    private final Clock clock;

    public AccountInvitationStatusHandler(AccountInvitationRepository invitations, Clock clock) {
        this.invitations = invitations;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInvited(UUID userId) {
        return invitations.findByUserId(userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, InvitationStatus> statusAmong(Collection<UUID> userIds) {
        Instant now = clock.instant();
        return invitations.findByUserIds(userIds).entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                entry -> new InvitationStatus(entry.getValue().isExpiredAt(now), entry.getValue().expiresAt())));
    }
}
