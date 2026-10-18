package com.nido.api.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code color} is one of the six colours of an event, or none: the graphite of external sources. */
public record ExternalFeedRequest(@NotBlank @Size(max = 2000) String url, @Size(max = 100) String name,
                                  @Pattern(regexp = ExternalFeedRequest.EVENT_COLOR) String color) {

    /** The colours an event may wear (eventPalette.ts on the frontend). */
    public static final String EVENT_COLOR = "event-(violet|magenta|cyan|graphite|indigo|brique)";
}
