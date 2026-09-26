package com.nido.api.dashboard.infrastructure.web.dto;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.CardResult;
import com.nido.api.dashboard.domain.model.Dashboard;
import com.nido.api.space.domain.model.SpaceType;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The dashboard as the client reads it. {@code cards} omits a card with nothing relevant; its keys are
 * the lowercase card kinds, in card order.
 */
public record DashboardResponse(LocalDate date, SpaceType spaceType, boolean canWrite,
                                List<AttentionItemResponse> attention, Map<String, CardResultResponse> cards) {

    public static DashboardResponse from(Dashboard dashboard) {
        Map<String, CardResultResponse> cards = new LinkedHashMap<>();
        for (CardKind kind : CardKind.values()) {
            CardResult result = dashboard.cards().get(kind);
            if (kind.hasCard() && result != null) {
                cards.put(kind.name().toLowerCase(Locale.ROOT), CardResultResponse.from(result));
            }
        }
        return new DashboardResponse(dashboard.date(), dashboard.spaceType(), dashboard.canWrite(),
            dashboard.attention().stream().map(AttentionItemResponse::from).toList(), cards);
    }
}
