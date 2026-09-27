package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.PendingInvitations;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.space.application.port.in.ListMyInvitationsUseCase;
import com.nido.api.space.domain.model.ReceivedInvitationView;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reads the invitations waiting for the caller and lets {@link PendingInvitations} order them. They belong
 * to the account, not to the space being read, which is why they are found by the caller's e-mail — the
 * address invitations are sent to.
 */
@Component
public class InvitationsDashboardSource implements DashboardSource {

    private final ListMyInvitationsUseCase listMine;

    public InvitationsDashboardSource(ListMyInvitationsUseCase listMine) {
        this.listMine = listMine;
    }

    @Override
    public CardKind kind() {
        return CardKind.INVITATIONS;
    }

    @Override
    public SourceResult read(DashboardContext context) {
        List<AttentionItem.Invitation> invitations = listMine.listMine(context.callerEmail()).stream()
            .map(InvitationsDashboardSource::toItem)
            .toList();
        return PendingInvitations.of(invitations);
    }

    private static AttentionItem.Invitation toItem(ReceivedInvitationView view) {
        return new AttentionItem.Invitation(view.invitationId(), view.spaceName(), view.spaceGlyph(), view.spaceAccent(),
            view.role(), view.invitedByUsername(), view.expiresAt());
    }
}
