package com.nido.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Activation de l'application : le secret à scanner ou à saisir")
public record AppSetupResponse(
    @Schema(description = "URI otpauth:// à encoder en QR code", example = "otpauth://totp/Nido:jane%40example.fr?secret=BASE32SECRET&issuer=Nido") String otpauthUri,
    @Schema(description = "Secret en base32, à afficher si le scan est impossible", example = "JBSWY3DPEHPK3PXP") String secret
) {}
