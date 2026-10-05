package com.nido.api.mail.domain.model;

import java.util.List;

/** What came of a test mail. {@code detail}: what the server answered, for the administrator looking at the form. */
public sealed interface TestMailOutcome {
    record Sent() implements TestMailOutcome {}
    record Invalid(List<MailSettingsProblem> problems) implements TestMailOutcome {}
    record Failed(String detail) implements TestMailOutcome {}
}
