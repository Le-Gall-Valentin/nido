package com.nido.api.dashboard.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nido.api.dashboard.domain.model.CardResult;

/** {@code {"status":"OK","data":{…}}} or {@code {"status":"UNAVAILABLE"}} — never a null {@code data}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardResultResponse(String status, Object data) {

    static CardResultResponse from(CardResult result) {
        return switch (result) {
            case CardResult.Ok ok -> new CardResultResponse("OK", CardDataResponses.from(ok.card()));
            case CardResult.Unavailable unavailable -> new CardResultResponse("UNAVAILABLE", null);
        };
    }
}
