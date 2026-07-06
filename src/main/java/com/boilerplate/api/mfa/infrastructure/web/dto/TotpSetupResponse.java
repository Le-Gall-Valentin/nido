package com.boilerplate.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Données d'initialisation du TOTP à afficher à l'utilisateur")
public record TotpSetupResponse(
    @Schema(description = "URI otpauth:// à encoder en QR code pour l'application d'authentification (Google Authenticator, Authy…)", example = "otpauth://totp/Boilerplate:john.doe%40example.com?secret=BASE32SECRET&issuer=Boilerplate")
    String otpauthUri,

    @Schema(description = "Secret TOTP en base32 — à afficher en secours si le scan du QR code est impossible", example = "JBSWY3DPEHPK3PXP")
    String secret
) {}