package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * {@code cards} holds the cards that have something to show, and every kind whose source failed —
 * including a kind without a card, such as the invitations, so that the failure is never lost.
 */
public record Dashboard(LocalDate date, SpaceType spaceType, boolean canWrite,
                        List<AttentionItem> attention, Map<CardKind, CardResult> cards) {

    public Dashboard {
        attention = List.copyOf(attention);
        cards = Map.copyOf(cards);
    }

    /**
     * Whether every source answered. A failed source — even one without a card of its own — may have
     * kept items out of {@link #attention()}, so an empty list then does not mean "nothing to do".
     */
    public boolean complete() {
        return cards.values().stream().noneMatch(CardResult.Unavailable.class::isInstance);
    }
}
