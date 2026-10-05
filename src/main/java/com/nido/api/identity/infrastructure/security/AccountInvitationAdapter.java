package com.nido.api.identity.infrastructure.security;

import com.nido.api.authentication.application.port.in.AccountInvitationStatusQuery;
import com.nido.api.authentication.application.port.in.InviteAccountUseCase;
import com.nido.api.identity.domain.model.InvitationDelivery;
import com.nido.api.identity.domain.model.InvitationState;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccountInvitationAdapter implements AccountInvitationPort {

    private final InviteAccountUseCase inviteAccount;
    private final AccountInvitationStatusQuery invitationStatus;

    public AccountInvitationAdapter(InviteAccountUseCase inviteAccount, AccountInvitationStatusQuery invitationStatus) {
        this.inviteAccount = inviteAccount;
        this.invitationStatus = invitationStatus;
    }

    @Override
    public InvitationDelivery invite(UUID userId, String inviterName) {
        return switch (inviteAccount.invite(userId, inviterName)) {
            case com.nido.api.authentication.application.dto.InvitationDelivery.Mailed ignored ->
                new InvitationDelivery.Mailed();
            case com.nido.api.authentication.application.dto.InvitationDelivery.Link link ->
                new InvitationDelivery.Link(link.url());
        };
    }

    @Override
    public boolean isInvited(UUID userId) {
        return invitationStatus.isInvited(userId);
    }

    @Override
    public Map<UUID, InvitationState> invitationsAmong(Collection<UUID> userIds) {
        return invitationStatus.statusAmong(userIds).entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                entry -> new InvitationState(entry.getValue().expired(), entry.getValue().expiresAt())));
    }
}
