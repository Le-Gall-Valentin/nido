package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** {@code cards} holds only the cards that have something to show or whose source failed. */
public record Dashboard(LocalDate date, SpaceType spaceType, boolean canWrite,
                        List<AttentionItem> attention, Map<CardKind, CardResult> cards) {

    public Dashboard {
        attention = List.copyOf(attention);
        cards = Map.copyOf(cards);
    }
}
