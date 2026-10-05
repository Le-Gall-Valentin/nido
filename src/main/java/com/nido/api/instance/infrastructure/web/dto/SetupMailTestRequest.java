package com.nido.api.instance.infrastructure.web.dto;

import com.nido.api.shared.validation.LanguageCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SetupMailTestRequest(
    @NotBlank String code,
    @NotNull Map<String, String> mail,
    String publicUrl,
    @NotBlank @Email String recipient,
    @LanguageCode String language
) {
    @Override public String toString() { return "SetupMailTestRequest[code=***, publicUrl=" + publicUrl + "]"; }
}
