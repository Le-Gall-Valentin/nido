package com.nido.api.dashboard.domain.model;

import java.util.Comparator;
import java.util.List;

/**
 * The invitations waiting for the caller, as "À traiter" items — the one that expires soonest first.
 * They belong to the account, not to the space being read, and have no card of their own.
 */
public final class PendingInvitations {

    private PendingInvitations() {}

    public static SourceResult of(List<AttentionItem.Invitation> invitations) {
        return SourceResult.attentionOnly(invitations.stream()
            .sorted(Comparator.comparing(AttentionItem.Invitation::expiresAt))
            .<AttentionItem>map(invitation -> invitation)
            .toList());
    }
}
