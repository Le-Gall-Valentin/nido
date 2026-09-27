package com.nido.api.dashboard.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nido.api.dashboard.domain.model.CardResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** {@code {"status":"OK","data":{…}}} or {@code {"status":"UNAVAILABLE"}} — never a null {@code data}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardResultResponse(
    @Schema(allowableValues = {"OK", "UNAVAILABLE"}) String status,
    CardDataResponses.CardData data
) {

    static CardResultResponse from(CardResult result) {
        return switch (result) {
            case CardResult.Ok ok -> new CardResultResponse("OK", CardDataResponses.from(ok.card()));
            case CardResult.Unavailable unavailable -> new CardResultResponse("UNAVAILABLE", null);
        };
    }
}
