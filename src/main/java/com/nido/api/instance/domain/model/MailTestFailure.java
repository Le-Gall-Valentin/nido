package com.nido.api.instance.domain.model;

/** Why a test mail did not leave, and what the SMTP server answered when it answered (null otherwise). */
public record MailTestFailure(String reason, String serverReply) {}
