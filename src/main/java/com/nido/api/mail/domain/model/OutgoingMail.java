package com.nido.api.mail.domain.model;

/** A written mail and who it goes to — what the outbox stores and the transport sends. */
public record OutgoingMail(Recipient to, RenderedMail content) {
}
