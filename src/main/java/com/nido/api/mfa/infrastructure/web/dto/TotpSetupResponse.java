package com.nido.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Données d'initialisation du TOTP à afficher à l'utilisateur")
public record TotpSetupResponse(
    @Schema(description = "URI otpauth:// à encoder en QR code pour l'application d'authentification (Google Authenticator, Authy…)", example = "otpauth://totp/Nido:john.doe%40example.com?secret=BASE32SECRET&issuer=Nido")
    String otpauthUri,

    @Schema(description = "Secret TOTP en base32 — à afficher en secours si le scan du QR code est impossible", example = "JBSWY3DPEHPK3PXP")
    String secret
) {}