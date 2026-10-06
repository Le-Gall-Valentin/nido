package com.nido.api.authentication.application.dto;

/**
 * What the sign-in pages may offer on this installation, and whether a mail can leave at all — what the
 * administration tells an administrator about who will be told.
 */
public record AuthCapabilities(boolean passwordReset, boolean mail) {
}
