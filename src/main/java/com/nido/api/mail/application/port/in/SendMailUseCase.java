package com.nido.api.mail.application.port.in;

import com.nido.api.mail.domain.model.MailRequest;

/**
 * The one door to send a mail. {@code send} means <i>accepted for delivery</i>: the mail is committed
 * with the caller's transaction and delivered after it, so a rolled-back caller sends nothing. When
 * mail is switched off, it is accepted and dropped — callers never need to ask first.
 */
public interface SendMailUseCase {
    void send(MailRequest request);
}
