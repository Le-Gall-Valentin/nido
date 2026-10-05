package com.nido.api.instance.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** Everything the setup screen collected. {@code mail} null: mail left for later. */
public record CompleteSetupRequest(
    @NotBlank String code,
    @Valid @NotNull SetupAdminRequest admin,
    String publicUrl,
    Map<String, String> mail,
    boolean encryptionKeySaved
) {
    @Override public String toString() { return "CompleteSetupRequest[code=***, admin=" + admin + ", publicUrl=" + publicUrl + "]"; }
}
