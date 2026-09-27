package com.nido.api.dashboard.domain.model;

/**
 * What became of one card. A card with nothing relevant has no result at all — it is absent from
 * {@link Dashboard#cards()} — which is different from a card whose source failed.
 */
public sealed interface CardResult {

    record Ok(DashboardCard card) implements CardResult {}

    record Unavailable() implements CardResult {}
}
