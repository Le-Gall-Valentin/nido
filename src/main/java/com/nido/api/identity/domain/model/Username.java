package com.nido.api.identity.domain.model;

import java.util.Objects;

/**
 * A username as Nido accepts it: 3 to 50 characters once surrounding spaces are dropped, none of them
 * '@'. The '@' is refused because sign-in, "forgot password" and invitations read a typed value holding
 * one as an email address: such a username could never be reached, or could stand in front of somebody's
 * address. The name keeps the case it is given; comparisons ignore it.
 *
 * <p>The one definition of the rule: requests check it through {@code @ValidUsername}, the database
 * refuses an '@' too ({@code ck_users_username_no_at}), and the frontend mirrors it in
 * {@code shared/lib/usernamePolicy}.
 */
public record Username(String value) {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 50;

    public Username {
        Objects.requireNonNull(value, "username");
        value = value.strip();
        if (!fits(value)) {
            throw new IdentityException.InvalidUsername();
        }
    }

    /** Whether {@code candidate} would make a username — for callers that answer rather than throw. */
    public static boolean isValid(String candidate) {
        return candidate != null && fits(candidate.strip());
    }

    private static boolean fits(String name) {
        return name.length() >= MIN_LENGTH && name.length() <= MAX_LENGTH && name.indexOf('@') < 0;
    }
}
