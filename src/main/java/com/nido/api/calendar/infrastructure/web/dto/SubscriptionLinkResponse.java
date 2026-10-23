package com.nido.api.calendar.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.SubscriptionLink;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionLinkResponse(UUID id, String label, List<CalendarSourceType> sources, boolean onlyMine,
                                       Instant createdAt, Instant lastReadAt) {
    public static SubscriptionLinkResponse from(SubscriptionLink link) {
        return new SubscriptionLinkResponse(link.id(), link.label(), link.sources().stream().sorted().toList(),
            link.onlyMine(), link.createdAt(), link.lastReadAt());
    }
}
