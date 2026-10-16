package com.nido.api.calendar.domain.model;

/** A new address and its token — the one time the token exists outside the calendar application given it. */
public record CreatedSubscriptionLink(SubscriptionLink link, String token) {}
