package com.nido.api.authentication.application.dto;

/** What the sign-in pages may offer on this installation. */
public record AuthCapabilities(boolean passwordReset) {
}
