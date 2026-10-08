package com.nido.api.authentication.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nido.api.authentication.domain.model.MailCodeDelivery;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Le code de connexion envoyé par mail : parti, ou pas encore et pourquoi")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MailCodeResponse(
    @Schema(description = "Le code est parti", example = "true") boolean sent,
    @Schema(description = "Quand il est parti : secondes avant de pouvoir en demander un autre", example = "60", nullable = true) Long resendAfterSeconds,
    @Schema(description = "Quand il n'est pas parti : secondes avant de pouvoir le demander", example = "420", nullable = true) Long retryAfterSeconds
) {
    public static MailCodeResponse sent(long resendAfterSeconds) {
        return new MailCodeResponse(true, resendAfterSeconds, null);
    }

    public static MailCodeResponse of(MailCodeDelivery delivery) {
        return switch (delivery) {
            case MailCodeDelivery.Sent sent -> sent(sent.resendAfterSeconds());
            case MailCodeDelivery.TooSoon tooSoon -> new MailCodeResponse(false, null, tooSoon.retryAfterSeconds());
            case MailCodeDelivery.LimitReached limit -> new MailCodeResponse(false, null, limit.retryAfterSeconds());
            case MailCodeDelivery.Unavailable unavailable -> new MailCodeResponse(false, null, 0L);
        };
    }
}
