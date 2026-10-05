package com.nido.api.authentication.domain.model;

import java.time.Duration;

/** How long an invitation link lives, and where it lands. */
public final class AccountInvitationRules {

    /** A week: long enough for someone who reads their mail on weekends. */
    public static final Duration VALIDITY = Duration.ofDays(7);

    private AccountInvitationRules() {}

    /** The token follows a '#': that part never reaches a server, so no access log and no Referer keeps it. */
    public static String welcomePath(String rawToken) {
        return "/welcome#token=" + rawToken;
    }
}
