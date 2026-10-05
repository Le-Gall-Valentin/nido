package com.nido.api.mail.domain.model;

import java.util.List;

/**
 * What came of a test mail. A failure says why as a reason the pages word; {@code serverReply} is what
 * the server answered, kept only when it answered as an SMTP server does (null otherwise).
 */
public sealed interface TestMailOutcome {

    String AUTHENTICATION_FAILED = "authentication_failed";
    String REJECTED = "rejected";
    String CONNECTION_REFUSED = "connection_refused";
    String UNKNOWN_HOST = "unknown_host";
    String TIMEOUT = "timeout";
    String TLS_FAILED = "tls_failed";
    String NOT_AN_SMTP_SERVER = "not_an_smtp_server";
    String FAILED = "failed";

    record Sent() implements TestMailOutcome {}

    record Invalid(List<MailSettingsProblem> problems) implements TestMailOutcome {}

    record Failed(String reason, String serverReply) implements TestMailOutcome {}
}
