package com.nido.api.calendar.infrastructure.web.dto;

/** The one answer that carries the address: it is not kept anywhere it could be read again. */
public record CreatedSubscriptionLinkResponse(SubscriptionLinkResponse link, String url, String webcalUrl) {}
