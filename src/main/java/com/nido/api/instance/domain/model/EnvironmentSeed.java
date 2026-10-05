package com.nido.api.instance.domain.model;

import java.util.stream.Stream;

/** NIDO_SEED_USERNAME, NIDO_SEED_EMAIL, NIDO_SEED_PASSWORD — all three, or none. */
public record EnvironmentSeed(String username, String email, String password) {

    public boolean anyGiven() {
        return Stream.of(username, email, password).anyMatch(EnvironmentSeed::given);
    }

    public boolean allGiven() {
        return Stream.of(username, email, password).allMatch(EnvironmentSeed::given);
    }

    private static boolean given(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return "EnvironmentSeed[username=" + username + ", email=***, password=***]";
    }
}
