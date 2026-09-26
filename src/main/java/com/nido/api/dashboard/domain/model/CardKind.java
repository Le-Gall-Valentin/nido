package com.nido.api.dashboard.domain.model;

/** What a dashboard source produces. Every kind but INVITATIONS renders a card of its own. */
public enum CardKind {
    AGENDA(true), MENU(true), TASKS(true), FINANCE(true), SAVINGS(true), SHOPPING(true),
    /** Contributes "À traiter" items only: a failure leaves no card behind, since there is none. */
    INVITATIONS(false);

    private final boolean hasCard;

    CardKind(boolean hasCard) {
        this.hasCard = hasCard;
    }

    public boolean hasCard() {
        return hasCard;
    }
}
