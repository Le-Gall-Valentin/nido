package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.space.application.port.in.ListMyInvitationsUseCase;
import com.nido.api.space.domain.model.ReceivedInvitationView;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * The invitations waiting for the caller. They belong to the account, not to the space being read,
 * which is why they are found by the caller's e-mail — the address invitations are sent to.
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
        List<AttentionItem> invitations = listMine.listMine(context.callerEmail()).stream()
            .sorted(Comparator.comparing(ReceivedInvitationView::expiresAt))
            .<AttentionItem>map(view -> new AttentionItem.Invitation(view.invitationId(), view.spaceName(),
                view.spaceGlyph(), view.spaceAccent(), view.role(), view.invitedByUsername(), view.expiresAt()))
            .toList();
        return SourceResult.attentionOnly(invitations);
    }
}
