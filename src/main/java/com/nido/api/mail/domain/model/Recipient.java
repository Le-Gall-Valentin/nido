package com.nido.api.mail.domain.model;

/** Who receives a mail. The display name is optional; the address is checked when the mail is written. */
public record Recipient(String address, String displayName) {

    public Recipient {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("A recipient needs an address");
        }
    }
}
