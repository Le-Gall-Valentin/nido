package com.nido.api.identity.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nido.api.identity.domain.model.InvitationDelivery;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Comment l'invitation a été transmise")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvitationDeliveryResponse(
    @Schema(description = "`mail` : envoyée par mail ; `link` : à transmettre par l'administrateur", allowableValues = {"mail", "link"})
    String delivery,

    @Schema(description = "Le lien d'invitation, pour `link` seulement — affiché une fois, jamais réaffiché", nullable = true)
    String link
) {
    public static InvitationDeliveryResponse of(InvitationDelivery delivery) {
        return switch (delivery) {
            case InvitationDelivery.Mailed ignored -> new InvitationDeliveryResponse("mail", null);
            case InvitationDelivery.Link shown -> new InvitationDeliveryResponse("link", shown.url());
        };
    }

    /** Without the link: it carries a live token. */
    @Override
    public String toString() {
        return "InvitationDeliveryResponse[delivery=" + delivery + "]";
    }
}
