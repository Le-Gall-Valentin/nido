package com.nido.api.identity.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** What a profile change answers when it has more to say than 204. */
@Schema(oneOf = {ProfileUpdateResponse.EmailCodeSent.class, ProfileUpdateResponse.MailMethodRemoved.class})
public sealed interface ProfileUpdateResponse {

    @Schema(name = "ProfileEmailCodeSent", description = "Rien n'est enregistré : un code est parti à la nouvelle adresse")
    record EmailCodeSent(
        @Schema(description = "Toujours true", example = "true") boolean emailCodeRequired,
        @Schema(description = "La nouvelle adresse, où le code est parti", example = "jane.doe@example.fr") String sentTo,
        @Schema(description = "Secondes avant de pouvoir demander un nouveau code", example = "60") long resendAfterSeconds
    ) implements ProfileUpdateResponse {}

    @Schema(name = "ProfileMailMethodRemoved",
        description = "Enregistré pendant que l'envoi de mails est coupé : rien n'a pu prouver la nouvelle adresse, le code par mail est retiré")
    record MailMethodRemoved(
        @Schema(description = "Toujours true", example = "true") boolean mailMethodRemoved
    ) implements ProfileUpdateResponse {}
}
