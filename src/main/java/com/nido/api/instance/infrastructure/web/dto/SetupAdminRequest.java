package com.nido.api.instance.infrastructure.web.dto;

import com.nido.api.shared.validation.LanguageCode;
import com.nido.api.shared.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Le premier compte SUPER_ADMIN")
public record SetupAdminRequest(
    // Its rule is identity's, written once in the domain: a username it refuses comes back as INITIAL_ADMIN_REFUSED.
    @NotBlank String username,
    @NotBlank @Email @Size(max = 254) String email,
    @StrongPassword String password,
    @LanguageCode String language
) {
    @Override public String toString() { return "SetupAdminRequest[username=" + username + ", email=***, password=***, language=" + language + "]"; }
}
