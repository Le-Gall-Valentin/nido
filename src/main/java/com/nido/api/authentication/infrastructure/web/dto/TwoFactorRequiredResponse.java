package com.nido.api.authentication.infrastructure.web.dto;

import com.nido.api.authentication.domain.model.LoginResult;
import com.nido.api.shared.model.TwoFactorMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Le mot de passe est bon ; la connexion attend un second facteur")
public record TwoFactorRequiredResponse(
    @Schema(description = "Toujours true", example = "true") boolean twoFactorRequired,
    @Schema(description = "Nom d'utilisateur du compte, pour saluer la personne même si elle a tapé son adresse", example = "camille") String username,
    @Schema(description = "Les méthodes utilisables, l'application avant le mail ; plusieurs = la personne choisit") List<TwoFactorMethod> methods,
    @Schema(description = "L'adresse masquée, quand le mail est parmi les méthodes", example = "c••••••n@exemple.fr", nullable = true) String maskedEmail,
    @Schema(description = "Quand le mail est la seule méthode : le code envoyé par le serveur", nullable = true) MailCodeResponse mailCode
) {
    public static TwoFactorRequiredResponse of(LoginResult.TwoFactorRequired required) {
        return new TwoFactorRequiredResponse(true, required.username(), required.methods(), required.maskedEmail(),
            required.mailCode() == null ? null : MailCodeResponse.of(required.mailCode()));
    }
}
