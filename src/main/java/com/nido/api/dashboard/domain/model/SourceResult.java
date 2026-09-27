package com.nido.api.dashboard.domain.model;

import java.util.List;

/** One source's answer: its card — null when it has nothing relevant — and its "À traiter" items. */
public record SourceResult(DashboardCard card, List<AttentionItem> attention) {

    public SourceResult {
        attention = List.copyOf(attention);
    }

    public static SourceResult of(DashboardCard card) {
        return new SourceResult(card, List.of());
    }

    public static SourceResult of(DashboardCard card, List<AttentionItem> attention) {
        return new SourceResult(card, attention);
    }

    public static SourceResult attentionOnly(List<AttentionItem> attention) {
        return new SourceResult(null, attention);
    }

    public static SourceResult nothing() {
        return new SourceResult(null, List.of());
    }
}
